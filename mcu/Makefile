# FocusPod firmware - CH32X035G8U6 (RISC-V RV32IMAC)
#
# Toolchain: any bare-metal RISC-V GCC with rv32imac/ilp32 support.
#   - auto detected in PATH (riscv64-unknown-elf-, riscv-none-elf-,
#     riscv-none-embed- e.g. MounRiver Studio)
#   - or set CROSS=/path/to/prefix-
#   - the repository local toolchain (../.tools/root) is preferred if present

TARGET = FocusPod

#######################################
# Toolchain selection

REPO_TOOLS := $(wildcard ../.tools/root/usr/bin)
REPO_AS    := $(wildcard ../.tools/root/usr/lib/riscv64-unknown-elf/bin/as)

ifneq ($(REPO_TOOLS),)
  CROSS ?= $(REPO_TOOLS)/riscv64-unknown-elf-
else ifeq ($(CROSS),)
  CROSS := $(firstword $(foreach p,riscv64-unknown-elf- riscv-none-elf- riscv-none-embed-,\
    $(if $(shell command -v $(p)gcc 2>/dev/null),$(p),)))
endif
ifneq ($(REPO_AS),)
  # The Ubuntu cross gcc is configured with an absolute --with-as path;
  # -B redirects it to the locally extracted binutils.
  TOOLPREFIX := -B$(dir $(REPO_AS))
endif

ifeq ($(CROSS),)
  $(error No RISC-V bare-metal toolchain found. Install gcc-riscv64-unknown-elf (apt) or MounRiver Studio, or set CROSS=)
endif

CC      := $(CROSS)gcc
OBJCOPY := $(CROSS)objcopy
SIZE    := $(CROSS)size

WCHISP  ?= wchisp

#######################################
# Sources

APP_DIR     := App
DRV_DIR     := Driver
EVT_SRC     := hardware/EXAM/SRC

CFILES := \
	$(APP_DIR)/main.c \
	$(APP_DIR)/board.c \
	$(APP_DIR)/cc_power.c \
	$(APP_DIR)/time.c \
	$(APP_DIR)/delay.c \
	$(APP_DIR)/usbh_aoa.c \
	$(APP_DIR)/system_ch32x035.c \
	$(DRV_DIR)/ch32x035_usbfs_host.c \
	$(DRV_DIR)/libc_stubs.c \
	$(EVT_SRC)/Core/core_riscv.c \
	$(EVT_SRC)/Peripheral/src/ch32x035_rcc.c \
	$(EVT_SRC)/Peripheral/src/ch32x035_gpio.c \
	$(EVT_SRC)/Peripheral/src/ch32x035_adc.c \
	$(EVT_SRC)/Peripheral/src/ch32x035_tim.c \
	$(EVT_SRC)/Peripheral/src/ch32x035_usart.c \
	$(EVT_SRC)/Peripheral/src/ch32x035_flash.c

ASFILES := \
	$(EVT_SRC)/Startup/startup_ch32x035.S

LINKER := $(EVT_SRC)/Ld/Link.ld

#######################################
# Flags

CPUFLAGS := -march=rv32imac -mabi=ilp32 -msmall-data-limit=8 $(TOOLPREFIX)

INCFLAGS := \
	-I$(APP_DIR) \
	-I$(DRV_DIR) \
	-I$(EVT_SRC)/Peripheral/inc \
	-I$(EVT_SRC)/Core

CFLAGS := $(CPUFLAGS) -O2 -g3 -Wall -Wextra \
	-DUSE_STDPERIPH_DRIVER \
	-ffunction-sections -fdata-sections -fno-common \
	$(INCFLAGS)

ASFLAGS := $(CPUFLAGS) $(INCFLAGS)

BUILD_DIR := build

LDFLAGS := $(CPUFLAGS) -nostartfiles -nostdlib -static \
	-Wl,--gc-sections -T $(LINKER) \
	-Wl,-Map=$(BUILD_DIR)/$(TARGET).map \
	-lgcc

OBJS := $(patsubst %.c,$(BUILD_DIR)/%.o,$(CFILES)) \
	$(patsubst %.S,$(BUILD_DIR)/%.o,$(ASFILES))

#######################################
# Rules

.PHONY: all clean flash

all: $(TARGET).bin $(TARGET).hex
	$(SIZE) $(BUILD_DIR)/$(TARGET).elf

$(BUILD_DIR):
	mkdir -p $@

$(BUILD_DIR)/%.o: %.c
	@mkdir -p $(dir $@)
	$(CC) $(CFLAGS) -c $< -o $@

$(BUILD_DIR)/%.o: %.S
	@mkdir -p $(dir $@)
	$(CC) $(ASFLAGS) -c $< -o $@

$(BUILD_DIR)/$(TARGET).elf: $(OBJS) $(LINKER)
	$(CC) $(OBJS) $(LDFLAGS) -o $@

$(TARGET).bin: $(BUILD_DIR)/$(TARGET).elf
	$(OBJCOPY) -O binary $< $@

$(TARGET).hex: $(BUILD_DIR)/$(TARGET).elf
	$(OBJCOPY) -O ihex $< $@

# USB ISP flashing: hold SW1 while powering the board from J2 (see READEM.md)
flash: $(TARGET).bin
	$(WCHISP) flash $<

clean:
	rm -rf $(BUILD_DIR) $(TARGET).bin $(TARGET).hex
