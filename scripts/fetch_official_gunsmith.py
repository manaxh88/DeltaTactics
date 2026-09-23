import urllib.request
import urllib.parse
import json
import os
import sys
import time

sys.stdout.reconfigure(encoding='utf-8')
headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'}

def map_category(second_class, name=""):
    if any(k in name for k in ['狙击步枪', 'AWM', 'M700', 'SV-98']):
        return "狙击步枪"
    if any(k in name for k in ['射手步枪', 'VSS', 'SVD', 'M14', 'Mini-14', 'SR-25']):
        return "射手步枪"
    if any(k in name for k in ['轻机枪', '机枪', '霰弹枪', 'M249', 'PKM', '725']):
        return "轻机枪/霰弹"
    if any(k in name for k in ['冲锋枪', 'SMG', 'UZI', 'MP5', 'Vector', 'P90', '勇士', '野牛', 'MK4']):
        return "冲锋枪"
    if any(k in name for k in ['突击步枪', '战斗步枪', 'CAR-15', 'AKS-74U', 'AR57', 'M4A1', 'K416', 'M7', 'AKM', 'AS Val', 'SG552', 'QBZ', 'AUG', 'G3', 'K437', 'PTR-32', 'MCX', 'MDR', 'ASh-12', 'AK-12']):
        return "突击步枪"
    if "步枪" in second_class:
        return "突击步枪"
    if "冲锋" in second_class:
        return "冲锋枪"
    if "狙击" in second_class:
        return "狙击步枪"
    if "射手" in second_class:
        return "射手步枪"
    if "机枪" in second_class or "霰弹" in second_class:
        return "轻机枪/霰弹"
    return "手枪/特种"

def clean_html_tags(text):
    if not text:
        return ""
    import re
    return re.sub(r'<[^>]+>', '', text).strip()

def main():
    print("=== Fetching Official Gunsmith Builds ===")
    
    # 1. Fetch weapon list
    url_guns = 'https://www.shushu.fan/api/guns-code/guns-list'
    req = urllib.request.Request(url_guns, headers=headers)
    guns = json.loads(urllib.request.urlopen(req, timeout=10).read().decode('utf-8')).get('data', [])
    print(f"Fetched {len(guns)} weapons from guns-list.")

    all_builds = []
    seen_codes = set()

    for idx, g in enumerate(guns):
        name = g.get('objectName')
        sec_class = g.get('secondClassCN', '')
        category = map_category(sec_class, name)
        gun_base_pic = g.get('pic', '')
        
        q = urllib.parse.quote(name)
        url_build = f'https://www.shushu.fan/api/guns-code/official?weaponNames={q}&limit=3'
        try:
            r = urllib.request.Request(url_build, headers=headers)
            res = json.loads(urllib.request.urlopen(r, timeout=10).read().decode('utf-8'))
            build_list = res.get('data', {}).get('list', [])
            
            for b_idx, b in enumerate(build_list):
                code = b.get('solutionCode', '').strip()
                if not code:
                    continue
                if '-' not in code:
                    code = f"{name}-烽火地带-{code}"
                if code in seen_codes:
                    continue
                seen_codes.add(code)
                
                # Image: prefer full-mod transparent render previewPic, fallback to prePreviewPic, fallback to gun_base_pic
                img = b.get('previewPic') or b.get('prePreviewPic') or gun_base_pic
                author = b.get('authorNickname', '官方精选')
                build_title = b.get('name', f"{name} 精选改装")
                price = b.get('price', 0)
                desc = clean_html_tags(b.get('authorComment', ''))
                
                # Accessories list
                accessories = []
                for acc in b.get('accessoryDetail', []):
                    acc_name = acc.get('name')
                    if acc_name and acc_name not in accessories:
                        accessories.append(acc_name)
                
                # Extract pros or tags
                tags = [t.get('name') for t in b.get('tagDetail', []) if t.get('name')]
                if not tags:
                    tags = ["官方精选", "排位推荐"]
                
                build_item = {
                    "id": f"build_{b.get('id', int(time.time()*1000 + idx))}",
                    "gunName": name,
                    "roleName": build_title,
                    "category": category,
                    "caliber": b.get('armsDetail', {}).get('gunDetail', {}).get('caliber', '').replace('ammo', '').strip() or "通用口径",
                    "buildCode": code,
                    "imageUrl": img,
                    "gunBasePic": gun_base_pic,
                    "specs": f"预估造价 {(price // 10000)}.{((price % 10000) // 1000)}万 • 控枪稳定性高" if price > 0 else "官方严选调校",
                    "description": desc or f"由创作者【{author}】调校分享的实战满改方案，兼顾后坐力控制与开镜机动性。",
                    "pros": tags[:4],
                    "keyAccessories": accessories[:6] if accessories else ["战术长枪管", "专用消音器", "光学瞄具", "扩容弹匣"],
                    "author": author,
                    "price": price
                }
                all_builds.append(build_item)
                
            print(f"[{idx+1}/{len(guns)}] {name}: processed {len(build_list)} builds")
        except Exception as e:
            print(f"[{idx+1}/{len(guns)}] Error fetching {name}: {e}")
        time.sleep(0.04)

    print(f"\nTotal builds collected: {len(all_builds)}")
    
    # Save to app/src/main/assets/gunsmith_official_builds.json
    script_dir = os.path.dirname(os.path.abspath(__file__))
    assets_dir = os.path.join(script_dir, "..", "app", "src", "main", "assets")
    os.makedirs(assets_dir, exist_ok=True)
    out_file = os.path.join(assets_dir, "gunsmith_official_builds.json")
    
    with open(out_file, "w", encoding="utf-8") as f:
        json.dump(all_builds, f, ensure_ascii=False, indent=2)
        
    print(f"Saved to {out_file} (size: {os.path.getsize(out_file)} bytes)")

if __name__ == '__main__':
    main()
