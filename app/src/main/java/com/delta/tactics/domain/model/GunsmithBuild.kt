package com.delta.tactics.domain.model

/**
 * 热门改枪配装模型（抄作业数据包）
 */
data class GunsmithBuild(
    val id: String,
    val gunName: String,
    val roleName: String,               // 方案定位（如 "稳定激光"、"远程点名"、"贴脸爆发"）
    val category: String,               // "突击步枪", "冲锋枪", "狙击步枪", "射手步枪"
    val caliber: String,                // 口径规格（如 "5.56×45mm"）
    val buildCode: String,              // 官方游戏改枪码
    val imageUrl: String,               // 官方透明高清武器图
    val specs: String,                  // 核心参数（如 "后坐力 -18% • 射程 42m"）
    val description: String,            // 方案设计初衷与实战打法
    val pros: List<String>,             // 优势标签
    val keyAccessories: List<String>    // 关键核心配件推荐清单
)

/**
 * 武器库官方图与热门改枪码方案提供类
 */
object GunsmithBuildRepository {

    val POPULAR_BUILDS: List<GunsmithBuild> = listOf(
        GunsmithBuild(
            id = "m4a1_laser",
            gunName = "M4A1 突击步枪",
            roleName = "全面突击 • 稳定激光",
            category = "突击步枪",
            caliber = "5.56×45mm",
            buildCode = "M4A1突击步枪-烽火地带-6H83DTG064GDC3CON5NTH",
            imageUrl = "https://playerhub.df.qq.com/playerhub/60004/object/18010000001.png",
            specs = "后坐力 -18% • 射程 42m • 射速 800 RPM",
            description = "全面压制水平与垂直后坐力，中远距离全自动无抖动，压枪门槛极低，适合大坝中距阵地交火。",
            pros = listOf("激光弹道", "极低后座", "新手友好", "泛用性强"),
            keyAccessories = listOf("幻影精选长枪管", "斜角减震握把", "全息光学瞄具", "45发扩容弹匣")
        ),
        GunsmithBuild(
            id = "ax50_sniper",
            gunName = "AX-50 狙击步枪",
            roleName = "长弓远狙 • 远程点名",
            category = "狙击步枪",
            caliber = ".50 BMG",
            buildCode = "AX-50狙击步枪-烽火地带-6HDN78802JEKFSVPUH1ID",
            imageUrl = "https://playerhub.df.qq.com/playerhub/60004/object/18060000001.png",
            specs = "开镜 0.32s • 射程 96m • 极限致命伤",
            description = "牺牲微量腰射换取极速开镜与高倍率弹道计算，长弓溪谷与巴克什高点狙击位一击必杀方案。",
            pros = listOf("单发毙命", "极速开镜", "超远射程", "高穿甲"),
            keyAccessories = listOf("灵眼12倍弹道狙击镜", "钛金轻量脚架", "战术消音枪口", "人体工学腮托")
        ),
        GunsmithBuild(
            id = "k416_meta",
            gunName = "K416 突击步枪",
            roleName = "烽火利器 • 火力压制",
            category = "突击步枪",
            caliber = "5.56×45mm",
            buildCode = "K416突击步枪-烽火地带-6HDQ3E802JEKFSVPUH1ID",
            imageUrl = "https://playerhub.df.qq.com/playerhub/60004/object/18010000013.png",
            specs = "射速 850 RPM • 控制 76 • 射程 50m",
            description = "主流高段位排位主力步枪，兼顾射速与连发精度，近距爆发与中距对枪均处于T0级别。",
            pros = listOf("T0射速", "稳定弹道", "控枪舒适", "破甲高效"),
            keyAccessories = listOf("竞赛重型枪管", "共振前握把", "ACOG四倍镜", "特种重型枪托")
        ),
        GunsmithBuild(
            id = "warrior_cqb",
            gunName = "勇士 冲锋枪",
            roleName = "贴脸强袭 • 高穿腰射",
            category = "冲锋枪",
            caliber = "9×19mm",
            buildCode = "勇士冲锋枪-烽火地带-6HDM5C802JEKFSVPUH1ID",
            imageUrl = "https://playerhub.df.qq.com/playerhub/60004/object/18020000009.png",
            specs = "机动 +25% • TTK 210ms • 贴脸秒杀",
            description = "航天基地室内战与狭窄楼道冲房神器，腰射散布拉到极致，近战开火即融化敌方护甲。",
            pros = listOf("极速TTK", "超高人机", "腰射激光", "室内王者"),
            keyAccessories = listOf("勇士海狸枪管", "快拔反射红点", "轻量镂空握把", "50发大弹鼓")
        ),
        GunsmithBuild(
            id = "mp5_speed",
            gunName = "MP5 冲锋枪",
            roleName = "极速跑打 • 灵活突防",
            category = "冲锋枪",
            caliber = "9×19mm",
            buildCode = "MP5冲锋枪-烽火地带-6H82EPG064GDC3CON5NTH",
            imageUrl = "https://playerhub.df.qq.com/playerhub/60004/object/18020000001.png",
            specs = "射速 900 RPM • 散布 -30% • 极速拔枪",
            description = "高移速高射速跑打流派，配合滑铲跳与身法干员（威龙/露娜）快速切入侧翼打敌措手不及。",
            pros = listOf("极高射速", "跑打极稳", "后座平缓", "手感丝滑"),
            keyAccessories = listOf("战术一体消音管", "垂直快拔握把", "微型紧凑红点", "折叠轻量枪托")
        ),
        GunsmithBuild(
            id = "vss_silent",
            gunName = "VSS 射手步枪",
            roleName = "隐秘暗杀 • 微声连发",
            category = "射手步枪",
            caliber = "9×39mm",
            buildCode = "VSS射手步枪-烽火地带-6HDJ1E802JEKFSVPUH1ID",
            imageUrl = "https://playerhub.df.qq.com/playerhub/60004/object/18050000003.png",
            specs = "自带微声 • 穿甲肉伤双高 • 射速 700 RPM",
            description = "自带一体消音器，开火雷达无显红点，配合重型亚音速弹中距离连发点射极具压制力。",
            pros = listOf("全图隐秘", "无声暗杀", "全自动连发", "破甲力强"),
            keyAccessories = listOf("4倍专用光学镜", "一体化轻量枪托", "30发加长弹匣", "侧挂战术红外")
        ),
        GunsmithBuild(
            id = "akm_heavy",
            gunName = "AKM 突击步枪",
            roleName = "硬核高伤 • 破甲暴击",
            category = "突击步枪",
            caliber = "7.62×39mm",
            buildCode = "AKM突击步枪-烽火地带-6H7V3KG01BP0VFI9IHJ1K",
            imageUrl = "https://playerhub.df.qq.com/playerhub/60004/object/18010000006.png",
            specs = "单发 41 肉伤 • 破甲卓越 • 压枪手感沉稳",
            description = "追求高单发威力的老手专属，3发碎甲击杀效率极高，适合精通压枪控弹道的中近距对枪手。",
            pros = listOf("单发高伤", "碎甲迅速", "威慑力大", "经济耐用"),
            keyAccessories = listOf("特种补偿器枪口", "战术导轨护木", "RK-0垂直握把", "防滑橡胶后握把")
        ),
        GunsmithBuild(
            id = "ash12_beast",
            gunName = "ASh-12 突击步枪",
            roleName = "大口径巨兽 • 近身碎甲",
            category = "突击步枪",
            caliber = "12.7×55mm",
            buildCode = "ASh-12突击步枪-烽火地带-6I7C3OG00NP4VKH52A7H4",
            imageUrl = "https://playerhub.df.qq.com/playerhub/60004/object/18010000012.png",
            specs = "单发 62 肉伤 • 碎甲 2 发入魂 • 威慑拉满",
            description = "发射亚音速重弹的近战推土机，两枪击破5级重甲，在据点争夺与撤离点防守中堪称绞肉机。",
            pros = listOf("毁灭级肉伤", "极限碎甲", "近距离霸凌", "重炮轰鸣"),
            keyAccessories = listOf("重型制退消焰器", "战术大倾角前握把", "全息近战镜", "20发双排重弹匣")
        ),
        GunsmithBuild(
            id = "m16a4_burst",
            gunName = "M16A4 突击步枪",
            roleName = "三发点射 • 激光稳定",
            category = "突击步枪",
            caliber = "5.56×45mm",
            buildCode = "M16A4突击步枪-烽火地带-6HD74A002JEKFSWPVI2JE",
            imageUrl = "https://playerhub.df.qq.com/playerhub/60004/object/18010000014.png",
            specs = "后坐 -26% • 射程 52m • 点射散布趋零",
            description = "三连发特化流派，后坐力几乎全消除，中远距离点名手感极佳，性价比极高的新老手通吃配装。",
            pros = listOf("超稳点射", "极远射程", "极高精度", "经济实惠"),
            keyAccessories = listOf("长步枪枪管", "竞技斜握把", "3倍突击瞄具", "40发轻量弹匣")
        ),
        GunsmithBuild(
            id = "uzi_laser",
            gunName = "UZI 冲锋枪",
            roleName = "超轻机动 • 极速跑打",
            category = "冲锋枪",
            caliber = "9×19mm",
            buildCode = "UZI冲锋枪-烽火地带-6I792IG00NP4VKH52A7H4",
            imageUrl = "https://playerhub.df.qq.com/playerhub/60004/object/18020000004.png",
            specs = "射速 950 RPM • 跑打移速 +20% • 腰射极紧",
            description = "极端追求机动性与室内腰射贴脸换甲，适合跑图搜刮、闪电战抢占高资源区撤离方案。",
            pros = listOf("机动天花板", "腰射极小", "射速恐怖", "快速换弹"),
            keyAccessories = listOf("微型消音器", "战术红外瞄具", "快速拔枪握把", "32发快拔弹匣")
        ),
        GunsmithBuild(
            id = "sv98_bolt",
            gunName = "SV-98 狙击步枪",
            roleName = "快速拉栓 • 致命首发",
            category = "狙击步枪",
            caliber = "7.62×54mm R",
            buildCode = "SV-98狙击步枪-烽火地带-6H849HG064GDC3CON5NTH",
            imageUrl = "https://playerhub.df.qq.com/playerhub/60004/object/18060000001.png",
            specs = "拉栓速度 +35% • 射程 88m • 极速开镜",
            description = "轻装游击狙击方案，大幅削减开镜与拉栓动作延迟，适合多点游走打靶与卡点偷袭。",
            pros = listOf("极速拉栓", "轻装机动", "手感干脆", "爆头必死"),
            keyAccessories = listOf("8倍战术狙击镜", "轻量化聚合物枪托", "战术消焰制退器", "快拔拉栓手柄")
        ),
        GunsmithBuild(
            id = "svd_marksman",
            gunName = "SVD 射手步枪",
            roleName = "俄系经典 • 暴力连点",
            category = "射手步枪",
            caliber = "7.62×54mm R",
            buildCode = "SVD射手步枪-烽火地带-6H874IG064GDC3CON5NTH",
            imageUrl = "https://playerhub.df.qq.com/playerhub/60004/object/18050000003.png",
            specs = "单发 54 肉伤 • 连点稳定 • 穿甲等级 5+",
            description = "单发高伤害半自动狙击，配合重装高穿甲弹药，两枪躯干直接击杀，中远距离统治力极强。",
            pros = listOf("超高连射伤害", "强力破甲", "中远霸主", "弹道下坠小"),
            keyAccessories = listOf("PSO-1专用瞄具", "加重狙击枪托", "战术消音器", "20发扩容弹匣")
        ),
        GunsmithBuild(
            id = "m250_lmg",
            gunName = "M250 轻机枪",
            roleName = "火力堡垒 • 掩体压制",
            category = "轻机枪/霰弹",
            caliber = "6.8×51mm",
            buildCode = "M250轻机枪-烽火地带-6HD89A002JEKFSWPVI2JE",
            imageUrl = "https://playerhub.df.qq.com/playerhub/60004/object/18040000003.png",
            specs = "100发大弹箱 • 持续压制 • 穿透掩体",
            description = "撤离点防守与团队架枪利器，大容量弹链支持全自动不间断扫射，对掩体后目标形成毁灭性穿透打击。",
            pros = listOf("持久压制", "掩体穿透", "弹药充足", "火力威慑"),
            keyAccessories = listOf("重型脚架", "战术补偿消焰器", "全息机枪镜", "100发弹药箱")
        ),
        GunsmithBuild(
            id = "m1014_shotgun",
            gunName = "M1014 霰弹枪",
            roleName = "破门利器 • 贴脸秒杀",
            category = "轻机枪/霰弹",
            caliber = "12 Gauge",
            buildCode = "M1014霰弹枪-烽火地带-6H859HG064GDC3CON5NTH",
            imageUrl = "https://playerhub.df.qq.com/playerhub/60004/object/18030000001.png",
            specs = "半自动连喷 • 8发鹿弹 • 贴脸即融化",
            description = "CQB室内遭遇战终极近战杀器，半自动连续倾泻鹿弹，近身一枪碎甲两枪带走。",
            pros = listOf("极速连喷", "近战秒杀", "防守核武", "威慑十足"),
            keyAccessories = listOf("加长弹仓管", "收束喉缩枪口", "战术快拔握把", "微型反射瞄具")
        )
    )

    /**
     * 根据武器名称获取对应官方图片 URL
     */
    fun getWeaponImageUrl(weaponName: String): String {
        val lower = weaponName.lowercase()
        return when {
            lower.contains("m4a1") || lower.contains("m4") -> "https://playerhub.df.qq.com/playerhub/60004/object/18010000001.png"
            lower.contains("k416") || lower.contains("416") -> "https://playerhub.df.qq.com/playerhub/60004/object/18010000013.png"
            lower.contains("ax50") || lower.contains("ax-50") -> "https://playerhub.df.qq.com/playerhub/60004/object/18060000001.png"
            lower.contains("勇士") -> "https://playerhub.df.qq.com/playerhub/60004/object/18020000009.png"
            lower.contains("mp5") -> "https://playerhub.df.qq.com/playerhub/60004/object/18020000001.png"
            lower.contains("vss") -> "https://playerhub.df.qq.com/playerhub/60004/object/18050000003.png"
            lower.contains("akm") -> "https://playerhub.df.qq.com/playerhub/60004/object/18010000006.png"
            lower.contains("ash") || lower.contains("ash-12") -> "https://playerhub.df.qq.com/playerhub/60004/object/18010000012.png"
            lower.contains("腾龙") -> "https://playerhub.df.qq.com/playerhub/60004/object/18010000038.png"
            lower.contains("uzi") -> "https://playerhub.df.qq.com/playerhub/60004/object/18020000004.png"
            lower.contains("m1014") || lower.contains("霰弹") -> "https://playerhub.df.qq.com/playerhub/60004/object/18030000001.png"
            lower.contains("m250") || lower.contains("机枪") -> "https://playerhub.df.qq.com/playerhub/60004/object/18040000003.png"
            lower.contains("m16") || lower.contains("m16a4") -> "https://playerhub.df.qq.com/playerhub/60004/object/18010000014.png"
            lower.contains("aks") || lower.contains("74u") -> "https://playerhub.df.qq.com/playerhub/60004/object/18010000010.png"
            lower.contains("沙漠之鹰") || lower.contains("沙鹰") -> "https://playerhub.df.qq.com/playerhub/60004/object/18070000004.png"
            else -> ""
        }
    }
}
