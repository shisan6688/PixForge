INSERT IGNORE INTO app_user (id, demo_key, display_name) VALUES (1, 'A', '演示用户 A'), (2, 'B', '演示用户 B');
INSERT IGNORE INTO template (id,category,title,description,cover_path,result_sample_path,required_image_count,accepted_mime_types,max_file_size_bytes,accent,badge) VALUES
('cute-effect','图片特效','可爱特效','让日常照片多一点俏皮。柔和贴纸、闪光和温暖色调，一键点亮可爱瞬间。','/templates/cute-effect-before-after.png','/templates/cute-effect-before-after.png',1,'image/jpeg,image/png',5242880,'#f8b7ca','热门'),
('ai-chibi','AI 写真','真人变 Q 版','保留人物神态与穿搭特征，模拟生成一张有趣又精致的 Q 版形象。','/templates/ai-chibi-before-after.png','/templates/ai-chibi-before-after.png',1,'image/jpeg,image/png',5242880,'#d6c6f7','NEW'),
('travel-postcard','创意设计','旅行明信片','把旅途中的风景与回忆做成一张独一无二的旅行明信片。','/templates/letizia-agosta-3TzHmifXX5k-unsplash.jpg','/templates/letizia-agosta-3TzHmifXX5k-unsplash.jpg',2,'image/jpeg,image/png',5242880,'#b7d9cf',NULL),
('film-memory','图片特效','胶片记忆','为照片增添细腻颗粒与怀旧色彩，收藏此刻的光与影。','/templates/sam-operchuck-LGeE_cTLoqY-unsplash.jpg','/templates/sam-operchuck-LGeE_cTLoqY-unsplash.jpg',1,'image/jpeg,image/png',5242880,'#d8c2ac',NULL),
('golden-hour','AI 写真','日落剪影','用一张人像照片，模拟黄昏光线下的电影感剪影。','/templates/cemrecan-yurtman-wih0v2QIySo-unsplash.jpg','/templates/cemrecan-yurtman-wih0v2QIySo-unsplash.jpg',1,'image/jpeg,image/png',5242880,'#b6d7d1',NULL),
('vintage-camera','创意设计','复古时光','组合两张照片，模拟制作一幅带有复古相机质感的回忆拼贴。','/templates/mustafi-numann-kz3pzTVrHtU-unsplash.jpg','/templates/mustafi-numann-kz3pzTVrHtU-unsplash.jpg',2,'image/jpeg,image/png',5242880,'#e1bc99',NULL),
('misty-portrait','AI 写真','晨雾人像','让人物融入轻盈的薄雾与柔和光影，模拟制作静谧氛围照片。','/templates/cemrecan-yurtman-vqTAlVvrp2M-unsplash.jpg','/templates/cemrecan-yurtman-vqTAlVvrp2M-unsplash.jpg',1,'image/jpeg,image/png',5242880,'#b4cfd1',NULL),
('urban-portrait','AI 写真','都市街拍','用一张人像照片，模拟制作暖调城市街头写真，呈现自然的光影与街景氛围。','/templates/pexels-bollieball-5189976.jpg','/templates/pexels-bollieball-5189976.jpg',1,'image/jpeg,image/png',5242880,'#cbb69f','NEW');
