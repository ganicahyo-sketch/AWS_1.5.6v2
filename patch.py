from pathlib import Path

root=Path('/mnt/data/cropfix')

# Central crop database: restores the selectable crop catalog while keeping manual entry.
cropdb='''package id.stasiuncuaca;

import java.util.Locale;

/** Central crop catalog used by Settings and Field Notes. Manual entry remains supported. */
public final class CropDatabase {
    private CropDatabase() {}

    public static final String MANUAL = "Lainnya / isi manual";

    /**
     * All general-crop profiles currently supported by AgronomyEngine.
     * Kelapa sawit is intentionally excluded from STASIUN CUACA v1.5.7.
     */
    public static final String[] CROPS = {
        "Padi", "Jagung", "Kedelai", "Cabai", "Tomat", "Singkong", "Ubi jalar",
        "Kentang", "Bawang merah", "Kacang tanah", "Kacang hijau", "Kacang panjang",
        "Kubis", "Pakcoy", "Sawi", "Selada", "Brokoli", "Kembang kol", "Kangkung",
        "Bayam", "Wortel", "Mentimun", "Terong", "Melon", "Semangka", "Kale / kailan",
        MANUAL
    };

    public static boolean isManual(String crop) {
        return crop != null && MANUAL.equalsIgnoreCase(crop.trim());
    }

    public static boolean isKnown(String crop) {
        if (crop == null) return false;
        String n = normalize(crop);
        for (String item : CROPS) {
            if (MANUAL.equals(item)) continue;
            if (normalize(item).equals(n)) return true;
        }
        return false;
    }

    public static String normalize(String crop) {
        if (crop == null) return "";
        return crop.toLowerCase(Locale.US).replace((char)160, ' ').replaceAll("\\s+", " ").trim();
    }

    /**
     * Tbase defaults for the GDD screening model (deg C).
     * Values are literature/model reference values, not cultivar-specific constants.
     */
    public static double tBase(String crop) {
        String c = AgronomyEngine.normalizeCrop(crop).toLowerCase(Locale.US);
        if (c.contains("padi") || c.contains("rice")) return 10.0;
        if (c.contains("jagung") || c.contains("maize") || c.contains("corn")) return 10.0;
        if (c.contains("kedelai") || c.contains("soy")) return 10.0;
        if (c.contains("cabai") || c.contains("chili") || c.contains("pepper")) return 10.0;
        if (c.contains("tomat") || c.contains("tomato")) return 7.0;
        if (c.contains("singkong") || c.contains("cassava")) return 10.0;
        if (c.contains("ubi jalar") || c.contains("sweet potato")) return 12.0;
        if (c.contains("kentang") || c.contains("potato")) return 2.0;
        if (c.contains("bawang") || c.contains("shallot") || c.contains("onion")) return 4.5;
        if (c.contains("kacang tanah") || c.contains("groundnut")) return 10.0;
        if (c.contains("kacang hijau") || c.contains("mungbean")) return 8.0;
        if (c.contains("kacang panjang") || c.contains("long bean")) return 10.0;
        if (c.contains("kubis") || c.equals("kol") || c.contains("cabbage")) return 4.5;
        if (c.contains("pakcoy") || c.contains("pokcoy") || c.contains("pak choi") || c.contains("bok choy")) return 4.5;
        if (c.contains("sawi") || c.contains("mustard green") || c.contains("caisim") || c.contains("caisin") || c.contains("choy sum")) return 5.0;
        if (c.contains("selada") || c.contains("lettuce")) return 4.0;
        if (c.contains("brokoli") || c.contains("broccoli")) return 4.5;
        if (c.contains("kembang kol") || c.contains("cauliflower")) return 4.5;
        if (c.contains("kangkung") || c.contains("water spinach")) return 10.0; // warm-season proxy; no robust crop-specific value found
        if (c.contains("bayam") || c.contains("amaranth") || c.contains("spinach")) return 7.0; // amaranth proxy; spinach itself is ~4 C
        if (c.contains("wortel") || c.contains("carrot")) return 6.0;
        if (c.contains("timun") || c.contains("mentimun") || c.contains("cucumber")) return 10.0;
        if (c.contains("terong") || c.contains("eggplant")) return 10.0;
        if (c.contains("melon")) return 10.0;
        if (c.contains("semangka") || c.contains("watermelon")) return 10.0;
        if (c.contains("kale") || c.contains("kailan")) return 4.5; // Brassica/cabbage proxy
        return Double.NaN;
    }

    public static String tBaseBasis(String crop) {
        String c = AgronomyEngine.normalizeCrop(crop).toLowerCase(Locale.US);
        if (c.contains("padi") || c.contains("rice") || c.contains("jagung") || c.contains("maize") || c.contains("corn")
                || c.contains("kedelai") || c.contains("soy") || c.contains("kacang tanah") || c.contains("groundnut")
                || c.contains("kacang hijau") || c.contains("mungbean") || c.contains("kacang panjang") || c.contains("long bean")
                || c.contains("ubi jalar") || c.contains("sweet potato") || c.contains("singkong") || c.contains("cassava")
                || c.contains("wortel") || c.contains("carrot") || c.contains("kentang") || c.contains("potato")
                || c.contains("kubis") || c.equals("kol") || c.contains("cabbage") || c.contains("selada") || c.contains("lettuce")
                || c.contains("brokoli") || c.contains("broccoli") || c.contains("kembang kol") || c.contains("cauliflower")
                || c.contains("mentimun") || c.contains("cucumber") || c.contains("terong") || c.contains("eggplant")
                || c.contains("melon") || c.contains("semangka") || c.contains("watermelon") || c.contains("tomat") || c.contains("tomato"))
            return "FAO56rev / review 2025; nilai indikatif, cek varietas/lokasi.";
        if (c.contains("cabai") || c.contains("chili") || c.contains("pepper")) return "Review threshold 2025; pepper Tbase sekitar 10 °C.";
        if (c.contains("bawang") || c.contains("shallot") || c.contains("onion")) return "FAO56rev onion 4,5 °C; shallot dipakai sebagai proxy.";
        if (c.contains("sawi") || c.contains("mustard green") || c.contains("caisim") || c.contains("caisin")) return "FAO56rev mustard 5 °C; didukung studi mustard.";
        if (c.contains("pakcoy") || c.contains("pokcoy") || c.contains("pak choi") || c.contains("bok choy")) return "Brassica/cabbage proxy 4,5 °C; verifikasi varietas.";
        if (c.contains("kale") || c.contains("kailan")) return "Brassica/cabbage proxy 4,5 °C; tidak ada nilai universal tunggal.";
        if (c.contains("kangkung") || c.contains("water spinach")) return "Proxy warm-season 10 °C; nilai crop-specific belum cukup kuat.";
        if (c.contains("bayam") || c.contains("amaranth")) return "Amaranth proxy 7 °C; bila yang dimaksud spinach gunakan ~4 °C.";
        return "Tidak ada nilai spesifik yang cukup kuat; isi manual bila tersedia.";
    }
}
'''
(root/'app/src/main/java/id/stasiuncuaca/CropDatabase.java').write_text(cropdb, encoding='utf-8')

# Field Notes: editable autocomplete selector.
p=root/'app/src/main/java/id/stasiuncuaca/FieldNotesActivity.java'
s=p.read_text(encoding='utf-8')
s=s.replace('import android.widget.ArrayAdapter;\n', 'import android.widget.ArrayAdapter;\nimport android.widget.AutoCompleteTextView;\n')
s=s.replace('private EditText date, time, crop, variety, plantDate, hst, area, observation, action, targetYield, phaseManual;', 'private EditText date, time, variety, plantDate, hst, area, observation, action, targetYield, phaseManual;\n    private AutoCompleteTextView crop;')
anchor='        crop = findViewById(R.id.fnCrop);\n'
insert='''        crop = findViewById(R.id.fnCrop);\n        ArrayAdapter<String> cropAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, CropDatabase.CROPS);\n        crop.setAdapter(cropAdapter);\n        crop.setThreshold(0);\n        crop.setOnItemClickListener((parent, view, position, id) -> {\n            String selected = String.valueOf(parent.getItemAtPosition(position));\n            if (CropDatabase.isManual(selected)) {\n                crop.setText(\"\");\n                crop.setHint(\"Ketik nama komoditas manual\");\n                crop.requestFocus();\n            } else {\n                crop.setText(AgronomyEngine.normalizeCrop(selected));\n                crop.setSelection(crop.length());\n                updateAutoPhase();\n            }\n        });\n        crop.setOnClickListener(v -> crop.showDropDown());\n'''
if anchor not in s: raise SystemExit('field anchor missing')
s=s.replace(anchor,insert,1)
p.write_text(s,encoding='utf-8')

# Settings: same central catalog and manual entry.
p=root/'app/src/main/java/id/stasiuncuaca/SettingsActivity.java'
s=p.read_text(encoding='utf-8')
s=s.replace('import android.widget.ArrayAdapter;\n', 'import android.widget.ArrayAdapter;\nimport android.widget.AutoCompleteTextView;\n')
s=s.replace('private EditText title, channel, readKey, aiKey, aiModel, geminiKey, geminiModel, openRouterKey, openRouterModel, crop, lat, lon, elev;', 'private EditText title, channel, readKey, aiKey, aiModel, geminiKey, geminiModel, openRouterKey, openRouterModel, lat, lon, elev;\n    private AutoCompleteTextView crop;')
anchor='        aiKey=findViewById(R.id.aiKey); aiModel=findViewById(R.id.aiModel); crop=findViewById(R.id.crop);\n'
insert='''        aiKey=findViewById(R.id.aiKey); aiModel=findViewById(R.id.aiModel); crop=findViewById(R.id.crop);\n        ArrayAdapter<String> cropAdapter=new ArrayAdapter<>(this,android.R.layout.simple_dropdown_item_1line,CropDatabase.CROPS);\n        crop.setAdapter(cropAdapter); crop.setThreshold(0);\n        crop.setOnItemClickListener((parent,view,position,id)->{\n            String selected=String.valueOf(parent.getItemAtPosition(position));\n            if(CropDatabase.isManual(selected)){ crop.setText(\"\"); crop.setHint(\"Ketik nama komoditas manual\"); crop.requestFocus(); }\n            else { crop.setText(AgronomyEngine.normalizeCrop(selected)); crop.setSelection(crop.length()); }\n        });\n        crop.setOnClickListener(v->crop.showDropDown());\n'''
if anchor not in s: raise SystemExit('settings anchor missing')
s=s.replace(anchor,insert,1)
p.write_text(s,encoding='utf-8')

# XML: replace EditText crop controls with editable AutoCompleteTextView, preserving styling.
for fn, old, new in [
('app/src/main/res/layout/activity_field_notes.xml', '''<EditText\n                    android:id="@+id/fnCrop"''', '''<AutoCompleteTextView\n                    android:id="@+id/fnCrop"'''),
('app/src/main/res/layout/activity_settings.xml', '<EditText android:id="@+id/crop"', '<AutoCompleteTextView android:id="@+id/crop"')]:
    p=root/fn; s=p.read_text(encoding='utf-8');
    if old not in s: raise SystemExit(f'XML anchor missing {fn}')
    s=s.replace(old,new,1)
    p.write_text(s,encoding='utf-8')

# Add explicit Tbase evidence to agronomy analysis and correct profile values.
p=root/'app/src/main/java/id/stasiuncuaca/AgronomyEngine.java'
s=p.read_text(encoding='utf-8')
repls={
'"Kedelai",95,5.5,7.0,20,30,5.0,20,40,8,15,80,140,25,40,15,10,src':'"Kedelai",95,5.5,7.0,20,30,5.0,20,40,8,15,80,140,25,40,15,10,src',
'"Tomat",110,5.5,7.0,18,30,2.5,20,40,10,20,100,180,90,60,40,10,src':'"Tomat",110,5.5,7.0,18,30,2.5,20,40,10,20,100,180,90,60,40,7,src',
'"Singkong",300,4.5,7.0,20,32,2.5,20,40,8,15,80,160,70,35,35,12,src':'"Singkong",300,4.5,7.0,20,32,2.5,20,40,8,15,80,160,70,35,35,10,src',
'"Kentang",100,5.0,6.5,15,25,1.7,20,40,10,20,100,180,105,70,35,7,src':'"Kentang",100,5.0,6.5,15,25,1.7,20,40,10,20,100,180,105,70,35,2,src',
'"Bawang merah",90,5.5,6.8,15,30,1.2,20,40,10,20,80,160,100,55,35,8,src':'"Bawang merah",90,5.5,6.8,15,30,1.2,20,40,10,20,80,160,100,55,35,4.5,src',
'"Pakcoy",45,5.5,6.8,18,28,1.5,20,40,8,15,70,130,90,40,80,5,':'"Pakcoy",45,5.5,6.8,18,28,1.5,20,40,8,15,70,130,90,40,80,4.5,',
'"Sawi",45,5.5,7.0,18,30,1.5,20,40,8,15,70,130,95,40,10,10,':'"Sawi",45,5.5,7.0,18,30,1.5,20,40,8,15,70,130,95,40,10,5,',
'"Selada",55,5.5,6.8,12,24,1.5,20,40,8,15,80,140,100,60,90,5,':'"Selada",55,5.5,6.8,12,24,1.5,20,40,8,15,80,140,100,60,90,4,',
'"Brokoli",85,5.5,7.0,15,24,1.7,20,40,10,20,80,150,110,70,80,7,':'"Brokoli",85,5.5,7.0,15,24,1.7,20,40,10,20,80,150,110,70,80,4.5,',
'"Kembang kol",85,5.5,7.0,15,24,1.7,20,40,10,20,80,150,110,70,80,7,':'"Kembang kol",85,5.5,7.0,15,24,1.7,20,40,10,20,80,150,110,70,80,4.5,',
'"Wortel",100,5.5,7.0,15,25,1.7,15,35,8,15,80,150,85,50,90,7,':'"Wortel",100,5.5,7.0,15,25,1.7,15,35,8,15,80,150,85,50,90,6,',
'"Terong",120,5.5,7.0,22,32,1.7,20,40,10,20,80,160,100,70,100,12,':'"Terong",120,5.5,7.0,22,32,1.7,20,40,10,20,80,160,100,70,100,10,',
'"Kale / kailan",65,5.5,7.0,15,26,1.7,20,40,10,20,80,150,95,55,60,7,':'"Kale / kailan",65,5.5,7.0,15,26,1.7,20,40,10,20,80,150,95,55,60,4.5,'
}
for a,b in repls.items():
    if a in s: s=s.replace(a,b)
    else: print('missing engine anchor',a)
# Add GDD/Tbase line after temperature status in AgronomyActivity instead of engine.
p.write_text(s,encoding='utf-8')

p=root/'app/src/main/java/id/stasiuncuaca/AgronomyActivity.java'
s=p.read_text(encoding='utf-8')
anchor='        s.append("Suhu ").append(show(temp)).append(" °C -> ").append(AgronomyEngine.temperatureStatus(temp,crop)).append("; kisaran profil ").append(show(cp.tempMin)).append("–").append(show(cp.tempMax)).append(" °C\\n");\n'
insert=anchor+'        s.append("Tbase/GDD: ").append(show(cp.tBase)).append(" °C • ").append(CropDatabase.tBaseBasis(crop)).append("\\n");\n'
if anchor not in s: raise SystemExit('AgronomyActivity anchor missing')
s=s.replace(anchor,insert,1)
p.write_text(s,encoding='utf-8')

# Documentation with source hierarchy and explicit manual-entry behavior.
doc=root/'CROP_PROFILES_SAYURAN.md'
d=doc.read_text(encoding='utf-8')
d += '''\n\n## FIX13 — katalog komoditas dan Tbase\n\nKatalog komoditas dipulihkan sebagai pilihan editable pada Pengaturan dan Catatan Lapangan. Opsi terakhir **Lainnya / isi manual** mengosongkan field dan pengguna dapat mengetik komoditas yang tidak tersedia.\n\nTbase dipakai hanya untuk screening thermal time/GDD. Nilai utama mengikuti tabel indikatif FAO56rev 2025/review threshold 2025; beberapa komoditas yang tidak mempunyai nilai crop-specific yang kuat memakai proxy famili/tipe tanaman dan diberi label tersebut. Tbase bukan nilai universal untuk semua varietas.\n\nDaftar utama: Padi 10°C; Jagung 10°C; Kedelai 10°C; Cabai 10°C; Tomat 7°C; Singkong 10°C; Ubi jalar 12°C; Kentang 2°C; Bawang merah 4,5°C (proxy onion/shallot); Kacang tanah 10°C; Kacang hijau 8°C; Kacang panjang 10°C (proxy bean); Kubis 4,5°C; Pakcoy 4,5°C (proxy Brassica/cabbage); Sawi 5°C; Selada 4°C; Brokoli 4,5°C; Kembang kol 4,5°C; Kangkung 10°C (proxy warm-season); Bayam 7°C bila dimaksud amaranth; Wortel 6°C; Mentimun 10°C; Terong 10°C; Melon 10°C; Semangka 10°C; Kale/kailan 4,5°C (proxy Brassica).\n\nSumber: FAO56rev/review 2025 untuk banyak tanaman; MRCC/OSU dan WSU untuk tabel GDD sayuran; studi mustard untuk Tbase 5°C; studi mungbean menunjukkan nilai genotype-specific sekitar 8,6–9,8°C sehingga aplikasi menggunakan 8°C sebagai nilai model indikatif.\n'''
doc.write_text(d,encoding='utf-8')

print('patched')
