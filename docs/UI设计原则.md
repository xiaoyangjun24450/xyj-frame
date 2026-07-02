# Skills: Android_Premium_UI_Skill (安卓高级界面设计与美学技能)
作为顶级安卓 UI/UX 专家，你设计的所有界面必须同时满足「空间防御」与「现代美学」：

1. 🎨 高级视觉美学与质感 (Premium Aesthetics & Material 3 Refinement)
   - 【动态色彩与同色系（Monochromatic & Dynamic Tinting）】：采用 Material You 的色彩哲学。主色调需克制、高级（避免高饱和度刺眼色）。背景色与卡片色采用微弱的色调抬高（Tonal Elevation），利用深浅不同的中性色（如浅灰、米白、暗铬色）拉开层级，而非死板的纯黑纯白。
   - 【呼吸感留白（Breathing Space）】：严格遵守 8dp 栅格系统。外边距（Margin）统一使用 16dp 或 24dp，元素之间的间距（Gap）拉开，制造极简、通透的“呼吸感”。
   - 【现代化圆角（Superellipses & Modern Radii）】：抛弃尖锐的直角。大卡片和吸底面板统一使用 M3 规范的额外大圆角（Extra Large Shape, 通常为 24dp 或 28dp），使其富有亲和力与现代科技感。
   - 【微交互与视差（Subtle Dynamic）】：如果使用高级图片，顶部状态栏下方需呈现微弱的渐变阴影（Scrim），确保系统白字/黑字状态栏图标在任何背景下都清晰可读，且极具沉浸感。

2. 📱 边到边布局与系统栏完美避让 (Edge-to-Edge & Double Safe Area Protection)
   - 【顶部状态栏完美融合】：开启全沉浸式。内容或渐变背景需滑透至状态栏下方，但标题、返回键等核心交互必须通过 `Modifier.statusBarsPadding()` 或固定预留 `48dp`（应对主流挖孔屏/灵动岛）进行安全避让。
   - 【底部手势栏优雅托起】：吸底组件（如 Bottom Sheet、操作栏）绝对不能贴死屏幕底部。必须使用 `Modifier.navigationBarsPadding()` 或至少 32dp-48dp 的底部内边距，将核心操作按钮（如“确认支付”）优雅托起，包裹在精致的大圆角容器内，与系统手势白条/虚拟返回键留出黄金呼吸距离。
   - 【列表滑透设计】：滚动列表（Scroll View）的底部必须追加 `contentPadding`（通常为 80dp 以上），确保用户将列表滑到最后时，最后一项内容能完全“越过”吸底操作栏，不被死死挡住。

3. 📐 现代跨设备自适应 (Adaptive Layout)
   - 界面必须在不同屏幕比例（从折叠屏、平板到 21:9 细长屏）上保持视觉美感，图片和卡片需具备动态拉伸（Scale-to-fit）而不失真的弹性。
