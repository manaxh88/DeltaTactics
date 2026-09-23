const https = require('https');
const http = require('http');
const crypto = require('crypto');
const fs = require('fs');
const path = require('path');

const USER_AGENT = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36';

function fetchUrl(url, headers = {}) {
    return new Promise((resolve, reject) => {
        const client = url.startsWith('https') ? https : http;
        client.get(url, { headers: { 'User-Agent': USER_AGENT, ...headers } }, (res) => {
            let data = '';
            res.on('data', chunk => data += chunk);
            res.on('end', () => resolve({ statusCode: res.statusCode, body: data }));
        }).on('error', reject);
    });
}

function normalizeCategory(gunName, rawCategory = '') {
    if (!gunName) gunName = '';
    if (gunName.includes('狙击步枪') || gunName.includes('AWM') || gunName.includes('M700') || gunName.includes('SV-98') || gunName.includes('R93') || gunName.includes('M82')) {
        return '狙击步枪';
    }
    if (gunName.includes('射手步枪') || gunName.includes('精准射手') || gunName.includes('VSS') || gunName.includes('SVD') || 
        gunName.includes('M14') || gunName.includes('Mini-14') || gunName.includes('SR-25') || gunName.includes('SR9') || 
        gunName.includes('SKS') || gunName.includes('PSG-1') || gunName.includes('SVCH')) {
        return '射手步枪';
    }
    if (gunName.includes('轻机枪') || gunName.includes('机枪') || gunName.includes('霰弹') || 
        gunName.includes('M249') || gunName.includes('PKM') || gunName.includes('725') || 
        gunName.includes('M250') || gunName.includes('QJB') || gunName.includes('M1014') || 
        gunName.includes('S12K') || gunName.includes('M870') || gunName.includes('FS-12')) {
        return '轻机枪/霰弹';
    }
    if (gunName.includes('冲锋枪') || gunName.includes('SMG') || gunName.includes('UZI') || 
        gunName.includes('MP5') || gunName.includes('Vector') || gunName.includes('P90') || 
        gunName.includes('勇士') || gunName.includes('野牛') || gunName.includes('MK4') || 
        gunName.includes('汤姆逊') || gunName.includes('MP7') || gunName.includes('SR-3M') || gunName.includes('维克托')) {
        return '冲锋枪';
    }
    if (gunName.includes('突击步枪') || gunName.includes('战斗步枪') || 
        gunName.includes('CAR-15') || gunName.includes('AKS-74U') || 
        gunName.includes('AR57') || gunName.includes('M4A1') ||
        gunName.includes('K416') || gunName.includes('M7') || 
        gunName.includes('AKM') || gunName.includes('AS Val') ||
        gunName.includes('SG552') || gunName.includes('QBZ') ||
        gunName.includes('AUG') || gunName.includes('G3') ||
        gunName.includes('K437') || gunName.includes('PTR-32') ||
        gunName.includes('MCX') || gunName.includes('MDR') ||
        gunName.includes('ASh-12') || gunName.includes('AK-12') ||
        gunName.includes('SCAR') || gunName.includes('FAMAS') ||
        gunName.includes('RM277')) {
        return '突击步枪';
    }
    if (rawCategory && rawCategory !== '手枪/特种' && rawCategory !== '其他') {
        if (rawCategory.includes('步枪')) return '突击步枪';
        if (rawCategory.includes('冲锋')) return '冲锋枪';
        if (rawCategory.includes('狙击')) return '狙击步枪';
        if (rawCategory.includes('射手')) return '射手步枪';
        if (rawCategory.includes('机枪') || rawCategory.includes('霰弹')) return '轻机枪/霰弹';
    }
    return (gunName.includes('手枪') || gunName.includes('93R') || gunName.includes('G18') || gunName.includes('沙漠之鹰') || gunName.includes('.357') || gunName.includes('QSZ') || gunName.includes('G17') || gunName.includes('M1911')) ? '手枪/特种' : '突击步枪';
}

function cleanHtmlTags(str) {
    if (!str) return '';
    return str.replace(/<[^>]+>/g, '').trim();
}

function resolveGunName(b, code, gunsDict) {
    let name = b.objectName || (b.armsDetail && b.armsDetail.objectName) || '';
    if (!name || name === '通用武器') {
        if (code && code.includes('-')) {
            name = code.split('-')[0].trim();
        }
    }
    if (!name || name === '通用武器') {
        if (b.name) {
            for (const [gName] of gunsDict) {
                if (b.name.includes(gName)) {
                    name = gName;
                    break;
                }
            }
        }
    }
    return name || 'M4A1突击步枪';
}

async function fetchShushuOfficial(allBuildsMap, gunsDict) {
    console.log('\n--- 1. Fetching from shushu.fan (Official Builds) ---');
    let page = 1;
    let totalCount = 0;
    while (true) {
        try {
            const url = `https://www.shushu.fan/api/guns-code/official?page=${page}&limit=50`;
            const res = await fetchUrl(url);
            const data = JSON.parse(res.body);
            if (!data.success || !data.data || !data.data.list) break;
            
            const list = data.data.list;
            totalCount = data.data.totalCount || totalCount;
            if (list.length === 0) break;

            for (const b of list) {
                let code = (b.solutionCode || '').trim();
                if (!code) continue;
                const gunName = resolveGunName(b, code, gunsDict);
                if (!code.includes('-')) {
                    code = `${gunName}-烽火地带-${code}`;
                }

                if (allBuildsMap.has(code)) continue;

                const secClass = (b.armsDetail && b.armsDetail.secondClassCN) || '';
                const category = normalizeCategory(gunName, secClass);
                
                const gunInfo = gunsDict.get(gunName) || {};
                let img = b.previewPic || b.prePreviewPic || (b.armsDetail && b.armsDetail.pic) || gunInfo.pic || '';

                const author = b.authorNickname || '官方认证方案';
                const title = b.name || `${gunName} 战术改装`;
                const price = b.price || 0;
                const desc = cleanHtmlTags(b.authorComment || '');

                const accessories = [];
                if (b.accessoryDetail && Array.isArray(b.accessoryDetail)) {
                    for (const acc of b.accessoryDetail) {
                        if (acc.name && !accessories.includes(acc.name)) {
                            accessories.push(acc.name);
                        }
                    }
                }

                const tags = [];
                if (b.tagDetail && Array.isArray(b.tagDetail)) {
                    for (const t of b.tagDetail) {
                        if (t.name) tags.push(t.name);
                    }
                }
                if (tags.length === 0) tags.push('官方认证', '排位推荐');

                allBuildsMap.set(code, {
                    id: `shushu_off_${b.id || allBuildsMap.size + 1}`,
                    gunName: gunName,
                    roleName: title,
                    category: category,
                    caliber: (b.armsDetail && b.armsDetail.gunDetail && b.armsDetail.gunDetail.caliber) ? b.armsDetail.gunDetail.caliber.replace('ammo', '').trim() : (gunInfo.caliber || '通用口径'),
                    buildCode: code,
                    imageUrl: img,
                    gunBasePic: (b.armsDetail && b.armsDetail.pic) || gunInfo.pic || '',
                    author: author,
                    price: price,
                    description: desc,
                    pros: tags.slice(0, 3),
                    keyAccessories: accessories.slice(0, 5),
                    source: 'shushu_official'
                });
            }

            console.log(`  Shushu Official Page ${page}: fetched ${list.length} items (Total recorded: ${allBuildsMap.size} / ${totalCount})`);
            page++;
            if (page > 30) break;
        } catch (e) {
            console.error(`  Error at shushu official page ${page}:`, e.message);
            break;
        }
    }
}

async function fetchShushuCommunity(allBuildsMap, gunsDict) {
    console.log('\n--- 2. Fetching from shushu.fan (Community Builds) ---');
    let page = 1;
    let totalCount = 0;
    while (true) {
        try {
            const url = `https://www.shushu.fan/api/guns-code?page=${page}&limit=50`;
            const res = await fetchUrl(url);
            const data = JSON.parse(res.body);
            if (!data.success || !data.data || !data.data.list) break;
            
            const list = data.data.list;
            totalCount = data.data.totalCount || totalCount;
            if (list.length === 0) break;

            for (const b of list) {
                let code = (b.solutionCode || '').trim();
                if (!code) continue;
                const gunName = resolveGunName(b, code, gunsDict);
                if (!code.includes('-')) {
                    code = `${gunName}-烽火地带-${code}`;
                }

                if (allBuildsMap.has(code)) continue;

                const secClass = (b.armsDetail && b.armsDetail.secondClassCN) || '';
                const category = normalizeCategory(gunName, secClass);
                
                const gunInfo = gunsDict.get(gunName) || {};
                let img = b.previewPic || b.prePreviewPic || (b.armsDetail && b.armsDetail.pic) || gunInfo.pic || '';

                const author = b.authorNickname || '社区精选';
                const title = b.name || `${gunName} 实用改装`;
                const price = b.price || 0;
                const desc = cleanHtmlTags(b.authorComment || b.description || '');

                const accessories = [];
                if (b.accessoryDetail && Array.isArray(b.accessoryDetail)) {
                    for (const acc of b.accessoryDetail) {
                        if (acc.name && !accessories.includes(acc.name)) {
                            accessories.push(acc.name);
                        }
                    }
                }

                const tags = [];
                if (b.tagDetail && Array.isArray(b.tagDetail)) {
                    for (const t of b.tagDetail) {
                        if (t.name) tags.push(t.name);
                    }
                }
                if (tags.length === 0) tags.push('社区热门', '实战调校');

                allBuildsMap.set(code, {
                    id: `shushu_com_${b.id || allBuildsMap.size + 1}`,
                    gunName: gunName,
                    roleName: title,
                    category: category,
                    caliber: (b.armsDetail && b.armsDetail.gunDetail && b.armsDetail.gunDetail.caliber) ? b.armsDetail.gunDetail.caliber.replace('ammo', '').trim() : (gunInfo.caliber || '通用口径'),
                    buildCode: code,
                    imageUrl: img,
                    gunBasePic: (b.armsDetail && b.armsDetail.pic) || gunInfo.pic || '',
                    author: author,
                    price: price,
                    description: desc,
                    pros: tags.slice(0, 3),
                    keyAccessories: accessories.slice(0, 5),
                    source: 'shushu_community'
                });
            }

            console.log(`  Shushu Community Page ${page}: fetched ${list.length} items (Total recorded: ${allBuildsMap.size} / ${totalCount})`);
            page++;
            if (page > 15) break;
        } catch (e) {
            console.error(`  Error at shushu community page ${page}:`, e.message);
            break;
        }
    }
}

async function fetchOrzice(allBuildsMap, gunsDict) {
    console.log('\n--- 3. Fetching from Orzice.com (/v/gun_gqm) ---');
    try {
        const pageRes = await fetchUrl('https://orzice.com/v/gun_gqm');
        const html = pageRes.body;

        const acMatch = html.match(/var\s+AC\s*=\s*"([^"]+)"/);
        const aaMatch = html.match(/var\s+AA\s*=\s*"([^"]+)"/);
        const abMatch = html.match(/var\s+AB\s*=\s*"([^"]+)"/);
        const timeMatch = html.match(/var\s+TimeUnix\s*=\s*(\d+)/);

        if (!acMatch || !aaMatch || !abMatch) {
            console.error("  Failed to extract Orzice security tokens.");
            return;
        }

        const AC = acMatch[1];
        const AA = aaMatch[1];
        const AB = abMatch[1];
        const TimeUnix = timeMatch ? parseInt(timeMatch[1]) : Math.floor(Date.now() / 1000);
        const secret = "私自使用，后果自负！我方保留起诉权利！";

        const key = Buffer.from(AA, 'utf8');
        const iv = Buffer.alloc(16, 0);
        Buffer.from(AB, 'utf8').copy(iv);

        let page = 1;
        let totalCount = 0;
        let addedCount = 0;

        while (true) {
            const param = `key1=全部&key2=全部&p=${page}&limit=50&top=0&top2=0&n=&solutionType=gun`;
            const h1 = crypto.createHash('md5').update(param + TimeUnix + AC, 'utf8').digest('hex');
            const token = crypto.createHash('md5').update(TimeUnix + h1 + secret, 'utf8').digest('hex');

            const encodedParam = encodeURI(param);
            const apiUrl = `https://orzice.com/api/sjz/gun_gqm?${encodedParam}&token=${token}&timestamp=${TimeUnix}`;

            const apiRes = await fetchUrl(apiUrl, { 'Referer': 'https://orzice.com/v/gun_gqm' });
            const json = JSON.parse(apiRes.body);

            if (json.code !== 0 || !json.data) {
                break;
            }

            totalCount = json.count || totalCount;

            const decipher = crypto.createDecipheriv('aes-256-cbc', key, iv);
            decipher.setAutoPadding(true);
            let decrypted = decipher.update(json.data, 'base64', 'utf8');
            decrypted += decipher.final('utf8');

            const list = JSON.parse(decrypted);
            if (!Array.isArray(list) || list.length === 0) break;

            for (const b of list) {
                let code = (b.solutionCode || '').trim();
                if (!code) continue;
                const gunName = resolveGunName(b, code, gunsDict);
                if (!code.includes('-')) {
                    code = `${gunName}-烽火地带-${code}`;
                }

                if (allBuildsMap.has(code)) continue;

                const category = normalizeCategory(gunName, b.m_type || '');
                const gunInfo = gunsDict.get(gunName) || {};
                const img = b.pic || gunInfo.pic || '';
                const author = b.authorNickname || 'Orzice鼠鼠精选';
                const title = b.name || `${gunName} 实战配装`;
                const price = b.price || 0;
                const desc = cleanHtmlTags(b.authorComment || '');

                const accessories = [];
                if (b.properties_data && Array.isArray(b.properties_data)) {
                    for (const p of b.properties_data) {
                        if (p.name && p.value !== undefined) {
                            accessories.push(`${p.name} ${p.value}`);
                        }
                    }
                }

                const tags = ['鼠鼠攻略', '实战验证'];
                if (b.likeNum > 0) tags.push(`👍 ${b.likeNum}`);

                allBuildsMap.set(code, {
                    id: `orzice_${b.id || allBuildsMap.size + 1}`,
                    gunName: gunName,
                    roleName: title,
                    category: category,
                    caliber: gunInfo.caliber || '通用口径',
                    buildCode: code,
                    imageUrl: img,
                    gunBasePic: gunInfo.pic || '',
                    author: author,
                    price: price,
                    description: desc,
                    pros: tags.slice(0, 3),
                    keyAccessories: accessories.slice(0, 5),
                    source: 'orzice'
                });
                addedCount++;
            }

            page++;
            if (page > 30) break;
        }
        console.log(`  Orzice fetched up to page ${page}. Total unique builds now: ${allBuildsMap.size}`);
    } catch (e) {
        console.error("  Error fetching Orzice:", e.message);
    }
}

async function main() {
    console.log("==================================================");
    console.log("🚀 STARTING DUAL-SITE GUNSMITH AGGREGATOR CRAWLER");
    console.log("Sources: shushu.fan (Official + Community) & orzice.com");
    console.log("==================================================");

    // 0. Fetch base guns list dictionary
    console.log("Fetching official weapons list (71 weapons)...");
    const gunsRes = await fetchUrl('https://www.shushu.fan/api/guns-code/guns-list');
    const gunsData = JSON.parse(gunsRes.body);
    const gunsDict = new Map();
    for (const g of (gunsData.data || [])) {
        gunsDict.set(g.objectName, g);
    }
    console.log(`Loaded ${gunsDict.size} base weapons metadata.`);

    const allBuildsMap = new Map();

    // 1. Shushu Official
    await fetchShushuOfficial(allBuildsMap, gunsDict);

    // 2. Shushu Community
    await fetchShushuCommunity(allBuildsMap, gunsDict);

    // 3. Orzice Gunsmith
    await fetchOrzice(allBuildsMap, gunsDict);

    const totalBuilds = Array.from(allBuildsMap.values());
    console.log("\n==================================================");
    console.log(`🎉 ALL SOURCES FETCHED! Total Unique Builds: ${totalBuilds.length}`);
    console.log("==================================================");

    // Verify images and gun names
    const emptyImg = totalBuilds.filter(b => !b.imageUrl);
    console.log(`Builds with valid images: ${totalBuilds.length - emptyImg.length} / ${totalBuilds.length}`);

    // Category breakdown
    const catMap = {};
    for (const b of totalBuilds) {
        catMap[b.category] = (catMap[b.category] || 0) + 1;
    }
    console.log("Category breakdown:", catMap);

    // Save to target assets json
    const targetFile = path.resolve(__dirname, '../app/src/main/assets/gunsmith_official_builds.json');
    fs.writeFileSync(targetFile, JSON.stringify(totalBuilds, null, 2), 'utf8');
    const stats = fs.statSync(targetFile);
    console.log(`\n✅ Saved ${totalBuilds.length} builds to ${targetFile} (${(stats.size / 1024).toFixed(1)} KB)`);
}

main().catch(console.error);
