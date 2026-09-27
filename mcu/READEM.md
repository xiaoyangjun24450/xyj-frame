# FocusPod MCU 固件（CH32X035G8U6）

MCU 已从 CH552 更换为 CH32X035G8U6（RISC-V RV32IMAC），固件按
[hardware/电路设计.md](hardware/电路设计.md) 完全重写：MCU 不再是 USB 设备，
而是 **USB 主机 + Android Open Accessory (AOA) 配件**，同时负责 Type-C 供电时序。

## 目录结构

```
mcu/
├── App/                    应用代码
│   ├── main.c              供电会话状态机（泄放/输入检测/附着检测/会话）
│   ├── board.c/.h          引脚定义与 IO（IN / OUT / U2EN / 调试串口）
│   ├── cc_power.c/.h       J1 充电器 3A 检测(ADC)、J2 180uA Rp 与手机 Rd 附着检测(USBPD)
│   ├── usbh_aoa.c/.h       USB 主机 + AOA 握手 + Bulk 应用协议
│   ├── time.c/.h           1ms 时基（TIM2 轮询，无中断）
│   ├── delay.c/.h          SysTick 忙等延时（供 WCH 主机驱动使用）
│   ├── system_ch32x035.c   48MHz HSI 时钟（来自 WCH EVT）
│   └── ch32x035_conf.h     外设库配置
├── Driver/
│   ├── ch32x035_usbfs_host.c/.h   WCH 官方 USB 主机底层驱动（取自 EVT HOST_KM）
│   ├── usb_host_config.h          主机驱动所需最小配置
│   ├── libc_stubs.c, string.h, stdint.h   无 newlib 时的最小 C 库
├── hardware/               WCH EVT 库（Core/Peripheral/Startup/Ld）与电路资料
└── Makefile
```

## 引脚分配（QFN28，详见 board.h）

| 引脚 | 网络 | 方向 | 功能 |
| --- | --- | --- | --- |
| PA0 | IN | 输入(内部下拉) | 干接点输入，READ_IN 返回 0/1 |
| PA1 | OUT | 输出 | Q1(P-MOS) 栅极，**低=导通**，默认高=关断 |
| PA2 | J1 CC1 | 模拟输入 | 充电器 3A 声明检测 (ADC ch2) |
| PA4 | J1 CC2 | 模拟输入 | 同上 (ADC ch4) |
| PA5 | U2EN | 输出 | RT9742 使能，高=给手机供电 |
| PC14/PC15 | J2 CC1/CC2 | USBPD | 180uA Rp（1.5A Source），Rd 附着检测 |
| PC16/PC17 | D-/D+ | USB | USB 主机 PHY（5V 供电配置），PC17 兼 BOOT |
| PB10 | DBG | 输出 | 调试串口 USART1 TX @115200（板上是空焊盘） |

## 固件时序（对应电路设计.md）

1. 上电：U2 关、Rp 关、OUT 关，等 **650ms** 泄放（R7/C3）。
2. ADC 轮询 J1 CC1/CC2，确认充电器 **3A 声明**（vRd≈1.7V，窗口 1.45–2.25V，3 次消抖）。
3. 使能 J2 180uA Rp，等手机 **Rd 附着**（CC 落入 0.66–1.23V 窗口，5 次消抖）后才开 U2。
4. USB 主机枚举手机 → AOA 握手（GET_PROTOCOL 51 / SEND_STRING 52 ×6 / START 53）
   → 手机以 VID 0x18D1 PID 0x2D00/0x2D01 重枚举 → 绑定两个 Bulk 端点。
5. 运行 Bulk 命令协议；输入能力丢失或手机拔出：同关 U2+Rp，等 650ms 后重新检测。
6. 链路在线但 **5 秒**无有效命令：强制 OUT 关断（安全看门狗，`APP_CMD_TIMEOUT_MS`）。

## Bulk 应用协议（AOA 配件接口，ASCII，`\n` 结尾）

配件声明字符串：manufacturer=`FocusPod`，model=`FocusPod`（Android 端按此匹配）。

| 手机 → MCU | MCU → 手机 | 说明 |
| --- | --- | --- |
| `SET_OUT 1` | `OK` | 打开功率输出（Q1 低有效） |
| `SET_OUT 0` | `OK` | 关闭功率输出 |
| `READ_IN` | `IN 1` / `IN 0` | 读干接点输入 |
| `PING` | `PONG` | 链路保活（也喂看门狗） |
| `RESET` | `OK` | 安全重启（关 U2/Rp→泄放→USER 模式复位） |
| 其他 | `ERR CMD` | 未知命令 |

任何合法命令都会刷新 5 秒看门狗。

## 构建环境

需要 RISC-V 裸机 GCC（rv32imac/ilp32）。三选一：

```bash
# 方式 A：Ubuntu/Debian 软件包
sudo apt install gcc-riscv64-unknown-elf binutils-riscv64-unknown-elf

# 方式 B：MounRiver Studio 自带工具链（riscv-none-embed-gcc）
# 方式 C：仓库本地工具链（免 root，见下）
```

仓库本地工具链方案（无需 sudo，Makefile 会自动优先使用 `../.tools/root`）：

```bash
cd <repo根目录>/.tools
apt download gcc-riscv64-unknown-elf binutils-riscv64-unknown-elf
mkdir -p root && for d in *.deb; do dpkg-deb -x "$d" root; done
```

## 编译与烧录

```bash
cd mcu
make            # 生成 FocusPod.bin / FocusPod.hex
make flash      # wchisp（pip install wchisp）；Windows 也可用 WCHISPTool 图形工具
```

**USB ISP 烧录**（板载按键方案）：按住 SW1，J2 接电脑 USB-A 口上电
（或 Type-C 口电脑时 J1 同时接充电器），进入 ROM BOOT 后松开 SW1，
用 WCHISPTool / wchisp 下载。运行时 J2 作手机口、烧录时作下载口，分时复用。

## Android App 适配要点

旧 `FocusPodUsbCommandSender`（USB Host API + EP0 vendor 请求 0x01）已不适用。
新固件下手机是 **USB 设备/AOA 配件**，App 应改用
[UsbAccessory / openAccessory](https://developer.android.com/develop/connectivity/usb/accessory)：

- 监听 `UsbManager.ACTION_USB_ACCESSORY_ATTACHED`，按 manufacturer/model = `FocusPod` 过滤；
- `openAccessory()` 取得两个 `FileDescriptor`（IN/OUT 即 bulk 端点）；
- 上述 ASCII 命令按行读写，替代原 `controlTransfer(0x41, 0x01, ...)`。

## 调试

PB10（USART1 TX，115200）输出状态日志：输入确认、CC 附着/分离、AOA 握手各阶段、
命令错误等。生产板上该焊盘未引出，接一根线即可观察。
