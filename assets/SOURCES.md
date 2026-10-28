# 示例素材来源

本目录保存已确认的 7 张图片，用于素材制作网站的模板展示和演示。素材文件随源码交付，页面运行时不依赖第三方图片链接。记录日期：2026-10-28。

## Unsplash 原图

以下 5 张图片由用户从对应 Unsplash 原图页下载，项目中保留原始文件名和图像内容。各原图页标注为可按 [Unsplash License](https://unsplash.com/license) 免费使用；本项目仍主动记录作者与来源。使用时应避免将照片中的可识别人物、品牌或作品误表述为对本项目的背书。

| 对话中的名称 | 项目文件 | 摄影作者 | 原图页面 |
| --- | --- | --- | --- |
| 暖色轮廓 | [`templates/cemrecan-yurtman-wih0v2QIySo-unsplash.jpg`](./templates/cemrecan-yurtman-wih0v2QIySo-unsplash.jpg) | Cemrecan Yurtman | [Silhouette of a person against bright sunlight](https://unsplash.com/photos/silhouette-of-a-person-against-bright-sunlight-wih0v2QIySo) |
| 冷色轮廓 | [`templates/cemrecan-yurtman-vqTAlVvrp2M-unsplash.jpg`](./templates/cemrecan-yurtman-vqTAlVvrp2M-unsplash.jpg) | Cemrecan Yurtman | [Silhouette of a person against a bright, hazy light](https://unsplash.com/photos/silhouette-of-a-person-against-a-bright-hazy-light-vqTAlVvrp2M) |
| 海边小镇 | [`templates/letizia-agosta-3TzHmifXX5k-unsplash.jpg`](./templates/letizia-agosta-3TzHmifXX5k-unsplash.jpg) | Letizia Agosta | [Village near the ocean](https://unsplash.com/photos/village-near-the-ocean-3TzHmifXX5k) |
| 颗粒感街景 | [`templates/sam-operchuck-LGeE_cTLoqY-unsplash.jpg`](./templates/sam-operchuck-LGeE_cTLoqY-unsplash.jpg) | Sam Operchuck | [A view of a street with houses](https://unsplash.com/photos/a-view-of-a-street-with-a-bunch-of-houses-in-the-background-LGeE_cTLoqY) |
| 暖光胶片相机 | [`templates/mustafi-numann-kz3pzTVrHtU-unsplash.jpg`](./templates/mustafi-numann-kz3pzTVrHtU-unsplash.jpg) | Mustafi Numann | [Vintage camera illuminated by warm light](https://unsplash.com/photos/vintage-camera-illuminated-by-warm-light-kz3pzTVrHtU) |

“暖色轮廓”是对话中选图时使用的名称；实际下载原图也偏青绿色。后续如需暖色封面，应基于原图另做调色版本并保留来源记录。

## AI 生成的演示图

以下 2 张图片通过 Codex 内置 `image_gen` 于 2026-10-28 生成，属于本项目的演示素材。它们用于说明模板效果；网站运行时的任务处理仍按笔试要求使用模拟生成器，不调用真实 AI 服务。AI 的使用过程还应在最终项目 README 中说明。

| 名称 | 项目文件 | 生成过程 |
| --- | --- | --- |
| 可爱特效前后对比 | [`templates/cute-effect-before-after.png`](./templates/cute-effect-before-after.png) | 先生成虚构猫咪图，再以该图为参考编辑为左右对比：左侧原图，右侧加入爱心、爪印和闪光。 |
| 真人变 Q 版前后对比 | [`templates/ai-chibi-before-after.png`](./templates/ai-chibi-before-after.png) | 生成虚构成年人物的左右对比：左侧写实肖像，右侧保留发型、眼镜和服装特征的 Q 版形象。没有使用真实人物照片。 |

### 生成提示词

可爱特效最终编辑提示词（参考图为此前用 `image_gen` 生成的猫咪图）：

> Use case: compositing. Asset type: preview-only before-and-after cover for a cute photo effect template. Input image: the supplied kitten image is the edit target and identity reference. Primary request: turn this into a clean side-by-side before/after comparison of the SAME orange-and-white kitten. Left half: the kitten as an ordinary natural photo, same pose and expression, simple soft peach background, with absolutely no hearts, stickers, sparkles, glowing outline, or decorative effect. Right half: the exact same kitten in the same pose and crop, with the cute effect applied: restrained hand-drawn pink hearts, tiny paw prints, subtle sparkles and a soft glow. Composition: two equal vertical panels, a neat subtle divider, matching framing and lighting, square overall image suitable for a template card. Maintain the kitten's fur pattern, face, eyes, paw position, scale and camera angle across both halves. No text, labels, logos, watermarks or UI controls.

真人变 Q 版提示词：

> Use case: stylized-concept. Asset type: preview-only before-and-after cover for an AI-style chibi transformation template in an image creation website. Primary request: a clear side-by-side transformation of the SAME fictional adult person. Left panel: natural photorealistic portrait of a cheerful young adult woman with shoulder-length dark brown hair, round glasses, a cream sweater and blue overalls, standing against a simple light neutral background. Right panel: unmistakable Q-version/chibi illustration of that exact same person with a large head and small body, matching dark brown hair, round glasses, cream sweater and blue overalls, friendly smile and matching pose. Composition: two equal vertical panels with a subtle clean divider; consistent subject placement, suitable as a square template card. Style: left true camera photo; right polished cute chibi character art, visually obvious transformation. Lighting: soft and bright. Constraints: one person per panel, same identity cues across both sides, no text, no logos, no watermark, no UI controls, no fantasy landscape.
