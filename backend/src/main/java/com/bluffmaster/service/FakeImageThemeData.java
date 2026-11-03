package com.bluffmaster.service;

import com.bluffmaster.model.FakeImageCategory;
import com.bluffmaster.model.FakeImageRegion;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * 各地區 × 類別的假圖標籤（用於生成預設圖；可放入 uploads/fake/ 真實照片覆蓋）。
 */
final class FakeImageThemeData {

    private static final Map<FakeImageRegion, Map<FakeImageCategory, String[]>> LABELS = new EnumMap<>(FakeImageRegion.class);

    static {
        put(FakeImageRegion.TAIWAN,
                food("滷肉飯", "牛肉麵", "珍珠奶茶", "蚵仔煎", "雞排", "滷味", "便當", "夜市小吃"),
                scenery("太魯閣", "日月潭", "阿里山", "墾丁", "九份", "清境", "花東", "北海岸"),
                culture("廟宇", "慶典", "原住民", "客家", "傳統市場", "老街", "民俗", "節慶"),
                architecture("101", "赤崁樓", "孔廟", "西門町", "眷村", "古蹟", "橋梁", "車站"),
                art("畫作", "雕塑", "展覽", "手作", "文創", "插畫", "攝影展", "裝置藝術"),
                daily("捷運", "機車", "超商", "書店", "咖啡廳", "公園", "陽台", "通勤"),
                shopping("蝦皮訂單", "網購", "3C", "服飾", "生活用品", "超市", "百貨", "特賣"));

        put(FakeImageRegion.JAPAN,
                food("壽司", "拉麵", "天婦羅", "章魚燒", "抹茶甜點", "居酒屋", "便當", "和牛"),
                scenery("富士山", "京都", "北海道", "櫻花", "箱根", "沖繩", "奈良", "東京塔"),
                culture("神社", "溫泉", "和服", "祭典", "茶道", "動漫", "傳統工藝", "庭園"),
                architecture("清水寺", "東京車站", "城堡", "鳥居", "摩天大樓", "町屋", "橋樑", "燈塔"),
                art("浮世繪", "陶藝", "花道", "展覽", "設計", "攝影", "工藝", "畫廊"),
                daily("新幹線", "便利店", "街道", "地鐵", "咖啡", "書店", "公園", "海邊"),
                shopping("藥妝", "伴手禮", "3C", "服飾", "雜貨", "百貨", "Outlet", "網購"));

        put(FakeImageRegion.KOREA,
                food("韓式烤肉", "泡菜", "炸雞", "部隊鍋", "拌飯", "年糕", "咖啡廳", "路邊小吃"),
                scenery("首爾", "濟州島", "釜山", "雪景", "海灘", "漢江", "傳統村落", "南山塔"),
                culture("韓服", "K-POP", "宮廷", "市集", "節慶", "傳統舞", "韓劇場景", "博物館"),
                architecture("景福宮", "東大門", "傳統韓屋", "現代建築", "教堂", "橋樑", "車站", "塔樓"),
                art("展覽", "塗鴉", "設計", "攝影", "工藝", "畫廊", "裝置", "收藏"),
                daily("地鐵", "便利店", "咖啡", "街景", "購物街", "公園", "海邊", "通勤"),
                shopping("化妝品", "服飾", "3C", "雜貨", "網購", "Outlet", "市場", "免稅"));

        put(FakeImageRegion.CHINA,
                food("小籠包", "火鍋", "烤鴨", "麵食", "串串", "早點", "川菜", "甜品"),
                scenery("長城", "張家界", "桂林", "西湖", "黃山", "九寨溝", "古城", "海濱"),
                culture("故宮", "書法", "武術", "茶藝", "傳統節慶", "胡同", "少數民族", "博物館"),
                architecture("天壇", "古城牆", "園林", "現代地標", "橋樑", "塔", "車站", "古鎮"),
                art("書畫", "陶瓷", "展覽", "雕塑", "工藝", "攝影", "設計", "畫廊"),
                daily("高鐵", "街道", "公園", "夜市", "咖啡", "書店", "通勤", "市集"),
                shopping("網購", "百貨", "3C", "服飾", "特產", "超市", "Outlet", "直播帶貨"));

        put(FakeImageRegion.HONG_KONG,
                food("港式茶餐廳", "點心", "雲吞麵", "蛋塔", "魚蛋", "燒臘", "甜品", "街頭小吃"),
                scenery("維港", "太平山", "離島", "夜景", "海灘", "公園", "天際線", "碼頭"),
                culture("廟宇", "節慶", "街市", "電影場景", "博物館", "傳統", "街頭", "海港"),
                architecture("中環", "天星碼頭", "摩天大樓", "舊建築", "橋樑", "車站", "商場", "公屋"),
                art("展覽", "街頭藝術", "設計", "攝影", "畫廊", "工藝", "裝置", "收藏"),
                daily("地鐵", "叮叮車", "便利店", "咖啡", "街道", "海濱", "公園", "通勤"),
                shopping("海港城", "藥妝", "3C", "服飾", "雜貨", "Outlet", "網購", "免稅"));

        put(FakeImageRegion.MACAU,
                food("葡式蛋塔", "豬扒包", "海鮮", "粥品", "甜品", "茶餐廳", "小吃", "自助餐"),
                scenery("大三巴", "路氹", "海濱", "夜景", "燈塔", "公園", "老城", "度假村"),
                culture("葡式建築", "廟宇", "節慶", "博物館", "賭場", "老街", "廣場", "混合文化"),
                architecture("大三巴", "議事亭", "教堂", "度假村", "橋樑", "塔樓", "現代建築", "燈塔"),
                art("展覽", "雕塑", "設計", "攝影", "畫廊", "工藝", "裝置", "收藏"),
                daily("巴士", "街道", "海邊", "咖啡", "公園", "購物", "通勤", "市集"),
                shopping("免稅", "手信", "3C", "服飾", "精品", "雜貨", "Outlet", "網購"));

        put(FakeImageRegion.THAILAND,
                food("Pad Thai", "冬陰功", "芒果飯", "路邊攤", "海鮮", "咖喱", "甜品", "夜市"),
                scenery("曼谷", "清邁", "普吉島", "海灘", "寺廟", "叢林", "水上市場", "夕陽"),
                culture("佛寺", "傳統舞", "市集", "節慶", "按摩", "大象", "水燈節", "博物館"),
                architecture("大皇宮", "寺廟", "現代商場", "橋樑", "塔", "度假村", "木屋", "燈塔"),
                art("手工藝", "展覽", "雕塑", "設計", "攝影", "畫廊", "塗鴉", "收藏"),
                daily("嘟嘟車", "便利店", "街景", "海邊", "咖啡", "公園", "市集", "通勤"),
                shopping("夜市", "藥妝", "服飾", "3C", "雜貨", "紀念品", "Outlet", "網購"));

        put(FakeImageRegion.VIETNAM,
                food("河粉", "法國麵包", "春捲", "咖啡", "海鮮", "路邊攤", "甜品", "火鍋"),
                scenery("下龍灣", "會安", "胡志明", "河內", "海灘", "稻田", "古城", "洞穴"),
                culture("廟宇", "法式風情", "市集", "節慶", "傳統服飾", "博物館", "水上生活", "老街"),
                architecture("教堂", "古城", "殖民建築", "橋樑", "塔", "現代大樓", "車站", "燈塔"),
                art("漆器", "展覽", "工藝", "攝影", "設計", "畫廊", "雕塑", "收藏"),
                daily("機車", "咖啡", "街景", "海邊", "公園", "市集", "通勤", "便利店"),
                shopping("市場", "服飾", "3C", "雜貨", "紀念品", "網購", "Outlet", "手工品"));

        put(FakeImageRegion.SINGAPORE,
                food("海南雞飯", "肉骨茶", "辣椒螃蟹", "叻沙", "咖椰吐司", "甜品", "小贩中心", "海鮮"),
                scenery("濱海灣", "聖淘沙", "花園", "夜景", "海灘", "公園", "城市", "摩天輪"),
                culture("多元文化", "節慶", "博物館", "街區", "傳統", "海港", "花車", "藝術節"),
                architecture("魚尾獅", "金沙", "甘榜", "橋樑", "教堂", "車站", "商場", "燈塔"),
                art("展覽", "設計", "雕塑", "攝影", "畫廊", "工藝", "裝置", "收藏"),
                daily("地鐵", "街道", "咖啡", "公園", "海邊", "通勤", "便利店", "市集"),
                shopping("烏節路", "免稅", "3C", "服飾", "藥妝", "雜貨", "Outlet", "網購"));

        put(FakeImageRegion.MALAYSIA,
                food("椰漿飯", "肉骨茶", "沙嗲", "榴槤", "印度煎餅", "海鮮", "甜品", "路邊攤"),
                scenery("雙子塔", "蘭卡威", "沙巴", "海灘", "雨林", "古城", "清真寺", "夕陽"),
                culture("多元文化", "節慶", "市集", "傳統", "博物館", "海島", "老街", "宗教"),
                architecture("雙子塔", "清真寺", "殖民建築", "橋樑", "塔", "車站", "度假村", "燈塔"),
                art("手工藝", "展覽", "設計", "攝影", "畫廊", "雕塑", "工藝", "收藏"),
                daily("街道", "咖啡", "海邊", "公園", "市集", "通勤", "便利店", "購物"),
                shopping("百貨", "免稅", "3C", "服飾", "雜貨", "Outlet", "網購", "紀念品"));

        put(FakeImageRegion.INDONESIA,
                food("峇里料理", "沙嗲", "炒飯", "海鮮", "路邊攤", "甜品", "咖啡", "辣味"),
                scenery("峇里", "雅加達", "火山", "海灘", "梯田", "叢林", "島嶼", "夕陽"),
                culture("廟宇", "傳統舞", "節慶", "市集", "手工藝", "博物館", "海島", "宗教"),
                architecture("寺廟", "皇宮", "度假村", "橋樑", "塔", "殖民建築", "燈塔", "現代"),
                art("木雕", "蠟染", "展覽", "雕塑", "設計", "攝影", "畫廊", "收藏"),
                daily("機車", "海邊", "咖啡", "街景", "公園", "市集", "通勤", "便利店"),
                shopping("市場", "服飾", "3C", "雜貨", "紀念品", "Outlet", "網購", "手工品"));

        put(FakeImageRegion.PHILIPPINES,
                food("海鮮", "烤豬", "芒果", "路邊攤", "甜品", "咖啡", "快餐", "辣味"),
                scenery("長灘島", "宿霧", "馬尼拉", "海灘", "火山", "島嶼", "稻田", "夕陽"),
                culture("節慶", "教堂", "市集", "傳統", "博物館", "海島", "街頭", "音樂"),
                architecture("教堂", "殖民建築", "度假村", "橋樑", "塔", "燈塔", "現代", "車站"),
                art("手工藝", "展覽", "設計", "攝影", "畫廊", "雕塑", "工藝", "收藏"),
                daily("吉普尼", "海邊", "咖啡", "街景", "公園", "市集", "通勤", "便利店"),
                shopping("商場", "服飾", "3C", "雜貨", "紀念品", "Outlet", "網購", "市場"));

        put(FakeImageRegion.USA,
                food("漢堡", "牛排", "炸雞", "Pizza", "甜甜圈", "BBQ", "早午餐", "墨西哥餐"),
                scenery("紐約", "大峽谷", "黃石", "加州", "國家公園", "海灘", "城市", "公路"),
                culture("音樂", "運動", "節慶", "博物館", "好萊塢", "街頭", "大學", "歷史"),
                architecture("帝國大廈", "金門橋", "白宮", "摩天大樓", "燈塔", "車站", "教堂", "別墅"),
                art("畫廊", "街頭藝術", "展覽", "雕塑", "設計", "攝影", "博物館", "收藏"),
                daily("公路", "咖啡", "超市", "街景", "公園", "海邊", "通勤", "健身房"),
                shopping("Amazon", "Outlet", "3C", "服飾", "超市", "精品", "網購", "Costco"));

        put(FakeImageRegion.CANADA,
                food("楓糖", "Poutine", "海鮮", "牛排", "早午餐", "甜品", "咖啡", "多國料理"),
                scenery("落基山", "尼加拉", "楓葉", "湖泊", "國家公園", "城市", "海灣", "雪景"),
                culture("冰球", "節慶", "博物館", "原住民", "街頭", "多元文化", "歷史", "藝術節"),
                architecture("CN塔", "城堡", "橋樑", "教堂", "燈塔", "車站", "現代", "木屋"),
                art("畫廊", "展覽", "雕塑", "設計", "攝影", "工藝", "裝置", "收藏"),
                daily("街道", "咖啡", "公園", "海邊", "超市", "通勤", "雪景", "市集"),
                shopping("Outlet", "3C", "服飾", "雜貨", "超市", "網購", "精品", "紀念品"));

        put(FakeImageRegion.UK,
                food("炸魚薯條", "下午茶", "英式早餐", "Pie", "啤酒", "咖喱", "甜品", "市集小吃"),
                scenery("倫敦", "蘇格蘭", "湖區", "海岸", "古堡", "鄉村", "城市", "花園"),
                culture("皇室", "博物館", "音樂", "足球", "節慶", "劇院", "歷史", "街頭"),
                architecture("大笨鐘", "城堡", "教堂", "橋樑", "燈塔", "車站", "塔橋", "古鎮"),
                art("畫廊", "展覽", "雕塑", "設計", "攝影", "工藝", "街頭藝術", "收藏"),
                daily("地鐵", "咖啡", "公園", "街景", "海邊", "通勤", "書店", "市集"),
                shopping("Harrods", "Outlet", "3C", "服飾", "雜貨", "網購", "精品", "紀念品"));

        put(FakeImageRegion.FRANCE,
                food("可頌", "法式料理", "奶酪", "紅酒", "馬卡龍", "麵包", "咖啡", "市集"),
                scenery("巴黎", "普羅旺斯", "蔚藍海岸", "阿爾卑斯", "盧瓦爾", "葡萄園", "海邊", "鄉村"),
                culture("博物館", "時尚", "藝術", "節慶", "歷史", "街頭", "葡萄酒", "建築"),
                architecture("艾菲爾", "凱旋門", "教堂", "城堡", "橋樑", "燈塔", "車站", "別墅"),
                art("羅浮宮", "畫廊", "雕塑", "展覽", "設計", "攝影", "工藝", "收藏"),
                daily("咖啡", "街道", "公園", "海邊", "市集", "通勤", "地鐵", "書店"),
                shopping("精品", "Outlet", "3C", "服飾", "雜貨", "網購", "香水", "紀念品"));

        put(FakeImageRegion.ITALY,
                food("Pizza", "義大利麵", "冰淇淋", "咖啡", "海鮮", "奶酪", "甜品", "市集"),
                scenery("羅馬", "威尼斯", "佛羅倫斯", "五漁村", "阿瑪菲", "托斯卡尼", "海邊", "雪山"),
                culture("古羅馬", "歌劇", "時尚", "節慶", "博物館", "歷史", "街頭", "藝術"),
                architecture("競技場", "教堂", "城堡", "橋樑", "塔", "燈塔", "車站", "廣場"),
                art("文藝復興", "畫廊", "雕塑", "展覽", "設計", "攝影", "工藝", "收藏"),
                daily("機車", "咖啡", "街道", "海邊", "公園", "市集", "通勤", "書店"),
                shopping("精品", "Outlet", "3C", "服飾", "皮革", "網購", "雜貨", "紀念品"));

        put(FakeImageRegion.GERMANY,
                food("香腸", "豬腳", "啤酒", "麵包", "甜品", "咖啡", "聖誕市集", "料理"),
                scenery("新天鵝堡", "黑森林", "柏林", "萊茵河", "城市", "雪景", "湖泊", "鄉村"),
                culture("啤酒節", "博物館", "音樂", "歷史", "節慶", "足球", "街頭", "聖誕"),
                architecture("城堡", "教堂", "橋樑", "車站", "燈塔", "現代", "古鎮", "塔樓"),
                art("畫廊", "展覽", "雕塑", "設計", "攝影", "工藝", "街頭藝術", "收藏"),
                daily("地鐵", "咖啡", "公園", "街景", "市集", "通勤", "書店", "超市"),
                shopping("Outlet", "3C", "服飾", "雜貨", "網購", "精品", "紀念品", "超市"));

        put(FakeImageRegion.SPAIN,
                food("Tapas", "海鮮飯", "火腿", "甜品", "咖啡", "橄欖油", "小吃", "葡萄酒"),
                scenery("巴塞隆納", "馬德里", "海島", "海灘", "古城", "鬥牛", "夕陽", "山區"),
                culture("佛朗明哥", "節慶", "博物館", "歷史", "街頭", "足球", "建築", "藝術"),
                architecture("聖家堂", "城堡", "教堂", "橋樑", "燈塔", "車站", "廣場", "古鎮"),
                art("畫廊", "展覽", "雕塑", "高第", "設計", "攝影", "工藝", "收藏"),
                daily("街道", "咖啡", "海邊", "公園", "市集", "通勤", "地鐵", "書店"),
                shopping("Zara", "Outlet", "3C", "服飾", "雜貨", "網購", "精品", "紀念品"));

        put(FakeImageRegion.SWITZERLAND,
                food("奶酪", "巧克力", "火鍋", "麵包", "甜品", "咖啡", "料理", "葡萄酒"),
                scenery("阿爾卑斯", "少女峰", "湖泊", "雪景", "小鎮", "草原", "冰川", "瀑布"),
                culture("鐘錶", "滑雪", "節慶", "博物館", "多語文化", "歷史", "街頭", "藝術"),
                architecture("城堡", "教堂", "橋樑", "燈塔", "車站", "木屋", "現代", "塔樓"),
                art("畫廊", "展覽", "雕塑", "設計", "攝影", "工藝", "鐘錶", "收藏"),
                daily("火車", "咖啡", "湖泊", "雪景", "公園", "街景", "通勤", "市集"),
                shopping("鐘錶", "Outlet", "3C", "服飾", "巧克力", "網購", "精品", "紀念品"));

        put(FakeImageRegion.AUSTRALIA,
                food("牛排", "海鮮", "咖啡", "早午餐", "Pie", "BBQ", "甜品", "多國料理"),
                scenery("雪梨", "墨爾本", "大堡礁", "海灘", "袋鼠", "沙漠", "國家公園", "海岸"),
                culture("原住民", "運動", "節慶", "博物館", "海灘文化", "街頭", "歷史", "藝術"),
                architecture("歌劇院", "橋樑", "燈塔", "教堂", "車站", "現代", "海邊", "塔樓"),
                art("畫廊", "展覽", "雕塑", "設計", "攝影", "工藝", "街頭藝術", "收藏"),
                daily("海邊", "咖啡", "公園", "街景", "超市", "通勤", "燒烤", "市集"),
                shopping("Outlet", "3C", "服飾", "雜貨", "網購", "精品", "紀念品", "超市"));

        put(FakeImageRegion.NEW_ZEALAND,
                food("羊肉", "海鮮", "咖啡", "Pie", "甜品", "BBQ", "早午餐", "葡萄酒"),
                scenery("皇后鎮", "米佛峽灣", "草原", "雪山", "海灣", "火山", "湖泊", "海岸"),
                culture("毛利", "電影場景", "節慶", "博物館", "戶外", "歷史", "街頭", "藝術"),
                architecture("燈塔", "教堂", "橋樑", "車站", "木屋", "現代", "海邊", "塔樓"),
                art("畫廊", "展覽", "雕塑", "設計", "攝影", "工藝", "毛利藝術", "收藏"),
                daily("公路", "咖啡", "海邊", "公園", "農場", "街景", "通勤", "市集"),
                shopping("Outlet", "3C", "服飾", "羊毛", "網購", "雜貨", "紀念品", "精品"));

        // 其他地區 / 舊版 FOREIGN 共用
        Map<FakeImageCategory, String[]> otherLabels = buildCategoryMap(
                food("異國料理", "街頭小吃", "甜品", "咖啡", "海鮮", "BBQ", "早午餐", "特色菜"),
                scenery("海灘", "雪山", "古城", "國家公園", "城市", "沙漠", "島嶼", "夕陽"),
                culture("博物館", "節慶", "傳統", "市集", "宗教", "歷史", "街頭", "藝術"),
                architecture("教堂", "城堡", "橋樑", "燈塔", "車站", "塔", "現代", "古蹟"),
                art("畫廊", "展覽", "雕塑", "設計", "攝影", "工藝", "街頭藝術", "收藏"),
                daily("街道", "咖啡", "海邊", "公園", "市集", "通勤", "超市", "便利店"),
                shopping("Outlet", "3C", "服飾", "雜貨", "紀念品", "網購", "精品", "免稅"));
        LABELS.put(FakeImageRegion.OTHER, otherLabels);
        LABELS.put(FakeImageRegion.FOREIGN, otherLabels);
    }

    private FakeImageThemeData() {}

    static Map<FakeImageRegion, Map<FakeImageCategory, String[]>> all() {
        return LABELS;
    }

    static String[] get(FakeImageRegion region, FakeImageCategory category) {
        Map<FakeImageCategory, String[]> regionMap = LABELS.get(region);
        if (regionMap == null) {
            regionMap = LABELS.get(FakeImageRegion.OTHER);
        }
        return regionMap.get(category);
    }

    private static void put(FakeImageRegion region, String[] food, String[] scenery, String[] culture,
                          String[] architecture, String[] art, String[] daily, String[] shopping) {
        LABELS.put(region, buildCategoryMap(food, scenery, culture, architecture, art, daily, shopping));
    }

    private static Map<FakeImageCategory, String[]> buildCategoryMap(
            String[] food, String[] scenery, String[] culture, String[] architecture,
            String[] art, String[] daily, String[] shopping) {
        Map<FakeImageCategory, String[]> map = new EnumMap<>(FakeImageCategory.class);
        map.put(FakeImageCategory.FOOD, food);
        map.put(FakeImageCategory.SCENERY, scenery);
        map.put(FakeImageCategory.CULTURE, culture);
        map.put(FakeImageCategory.ARCHITECTURE, architecture);
        map.put(FakeImageCategory.ART, art);
        map.put(FakeImageCategory.DAILY_LIFE, daily);
        map.put(FakeImageCategory.SHOPPING, shopping);
        return map;
    }

    private static String[] food(String... labels) { return labels; }
    private static String[] scenery(String... labels) { return labels; }
    private static String[] culture(String... labels) { return labels; }
    private static String[] architecture(String... labels) { return labels; }
    private static String[] art(String... labels) { return labels; }
    private static String[] daily(String... labels) { return labels; }
    private static String[] shopping(String... labels) { return labels; }
}
