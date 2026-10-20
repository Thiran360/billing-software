package com.store.billing.util;

import com.store.billing.model.Product;

import java.util.*;
import java.util.regex.*;

public class ProductSearchUtil {

    private static final Map<String, String[]> ALIAS_MAP = new HashMap<>();

    static {
        // Brands & Oils
        ALIAS_MAP.put("gold", new String[]{"கோல்டு", "கோல்ட்", "gold"});
        ALIAS_MAP.put("winner", new String[]{"வின்னர்", "winner"});
        ALIAS_MAP.put("oil", new String[]{"ஆயில்", "எண்ணெய்", "எண்"});
        ALIAS_MAP.put("ayil", new String[]{"ஆயில்"});
        ALIAS_MAP.put("aayil", new String[]{"ஆயில்"});
        ALIAS_MAP.put("ennai", new String[]{"எண்ணெய்", "ஆயில்"});
        ALIAS_MAP.put("theepam", new String[]{"தீபம்", "தீப", "தீ.எண்"});
        ALIAS_MAP.put("deepam", new String[]{"தீபம்", "தீப", "தீ.எண்"});
        ALIAS_MAP.put("theep", new String[]{"தீபம்", "தீப", "தீ.எண்"});
        ALIAS_MAP.put("kalaimagal", new String[]{"கலைமகள்", "கலை"});
        ALIAS_MAP.put("kalai", new String[]{"கலைமகள்", "கலை"});
        ALIAS_MAP.put("idhayam", new String[]{"இதயம்"});
        ALIAS_MAP.put("ithayam", new String[]{"இதயம்"});
        ALIAS_MAP.put("mandhira", new String[]{"மந்திரா"});
        ALIAS_MAP.put("mantra", new String[]{"மந்திரா"});
        ALIAS_MAP.put("manthira", new String[]{"மந்திரா"});
        ALIAS_MAP.put("sunflower", new String[]{"சன்ஃபிளவர்", "சன்ப்ளவர்", "சன்"});
        ALIAS_MAP.put("groundnut", new String[]{"கடலை", "கடலை எண்ணெய்"});
        ALIAS_MAP.put("kadala", new String[]{"கடலை"});
        ALIAS_MAP.put("kadalai", new String[]{"கடலை"});
        ALIAS_MAP.put("gingelly", new String[]{"நல்லெண்ணெய்", "நல் எண்", "நல்.எண்"});
        ALIAS_MAP.put("nalen", new String[]{"நல்லெண்ணெய்", "நல் எண்", "நல்.எண்"});
        ALIAS_MAP.put("nalennai", new String[]{"நல்லெண்ணெய்", "நல் எண்", "நல்.எண்"});
        ALIAS_MAP.put("sesame", new String[]{"எள்ளு", "நல்லெண்ணெய்"});

        // Masalas, Spices & Non-Veg
        ALIAS_MAP.put("sambar", new String[]{"சாம்பார்", "சாம்பா"});
        ALIAS_MAP.put("chicken", new String[]{"சிக்கன்"});
        ALIAS_MAP.put("chilli", new String[]{"சில்லி", "மிளகாய்"});
        ALIAS_MAP.put("chilly", new String[]{"சில்லி", "மிளகாய்"});
        ALIAS_MAP.put("gobi", new String[]{"கோபி"});
        ALIAS_MAP.put("rasam", new String[]{"ரசம்"});
        ALIAS_MAP.put("biriyani", new String[]{"பிரியாணி"});
        ALIAS_MAP.put("briyani", new String[]{"பிரியாணி"});
        ALIAS_MAP.put("mutton", new String[]{"மட்டன்"});
        ALIAS_MAP.put("meen", new String[]{"மீன்"});
        ALIAS_MAP.put("fish", new String[]{"மீன்"});
        ALIAS_MAP.put("kulambu", new String[]{"குழம்பு"});
        ALIAS_MAP.put("kuzhambu", new String[]{"குழம்பு"});
        ALIAS_MAP.put("kari", new String[]{"கறி"});
        ALIAS_MAP.put("masala", new String[]{"மசாலா"});

        // Detergents, Cleaning & Soaps
        ALIAS_MAP.put("rin", new String[]{"ரின்"});
        ALIAS_MAP.put("ariel", new String[]{"ஏரியல்"});
        ALIAS_MAP.put("power", new String[]{"பவர்"});
        ALIAS_MAP.put("vim", new String[]{"விம்"});
        ALIAS_MAP.put("gel", new String[]{"ஜெல்"});
        ALIAS_MAP.put("lifebuoy", new String[]{"லைபாய்", "லைப்பாக்"});
        ALIAS_MAP.put("lifeboy", new String[]{"லைபாய்"});
        ALIAS_MAP.put("hamam", new String[]{"ஹமாம்"});
        ALIAS_MAP.put("lux", new String[]{"லக்ஸ்"});
        ALIAS_MAP.put("rexona", new String[]{"ரெக்சோனா"});
        ALIAS_MAP.put("santoor", new String[]{"சந்தூர்"});
        ALIAS_MAP.put("dettol", new String[]{"டெட்டால்"});
        ALIAS_MAP.put("medimix", new String[]{"மேடிமிக்ஸ்", "மெடிமிக்ஸ்"});
        ALIAS_MAP.put("mysore", new String[]{"மைசூர்"});
        ALIAS_MAP.put("sandal", new String[]{"சந்தனம்", "சாண்டல்"});
        ALIAS_MAP.put("surf", new String[]{"சர்ப்"});
        ALIAS_MAP.put("excel", new String[]{"எக்செல்"});
        ALIAS_MAP.put("tide", new String[]{"டைட்"});

        // Groceries & Provisions
        ALIAS_MAP.put("sugar", new String[]{"சீனி", "சர்க்கரை", "சுகர்"});
        ALIAS_MAP.put("seeni", new String[]{"சீனி", "சுகர்", "sugar"});
        ALIAS_MAP.put("sarkarai", new String[]{"சர்க்கரை", "sugar"});
        ALIAS_MAP.put("paruppu", new String[]{"பருப்பு"});
        ALIAS_MAP.put("dhal", new String[]{"பருப்பு"});
        ALIAS_MAP.put("dal", new String[]{"பருப்பு"});
        ALIAS_MAP.put("thuvaram", new String[]{"துவரம்", "துவரம் பருப்பு", "துவரை"});
        ALIAS_MAP.put("toor", new String[]{"துவரம்", "துவரம் பருப்பு"});
        ALIAS_MAP.put("ulunthu", new String[]{"உளுந்து", "உளுத்தம்"});
        ALIAS_MAP.put("ulundu", new String[]{"உளுந்து"});
        ALIAS_MAP.put("urad", new String[]{"உளுந்து"});
        ALIAS_MAP.put("paasi", new String[]{"பாசி", "பாசி பருப்பு"});
        ALIAS_MAP.put("pasi", new String[]{"பாசி", "பாசி பருப்பு"});
        ALIAS_MAP.put("moong", new String[]{"பாசி", "பாசி பருப்பு"});
        ALIAS_MAP.put("kadalai", new String[]{"கடலை"});
        ALIAS_MAP.put("channa", new String[]{"கடலை"});
        ALIAS_MAP.put("pottukadalai", new String[]{"பொட்டுக் கடலை", "பொட்டு"});
        ALIAS_MAP.put("pottu", new String[]{"பொட்டுக் கடலை", "பொட்டு"});
        ALIAS_MAP.put("kadugu", new String[]{"கடுகு"});
        ALIAS_MAP.put("mustard", new String[]{"கடுகு"});
        ALIAS_MAP.put("jeeragam", new String[]{"சீரகம்"});
        ALIAS_MAP.put("jeera", new String[]{"சீரகம்"});
        ALIAS_MAP.put("cumin", new String[]{"சீரகம்"});
        ALIAS_MAP.put("milagu", new String[]{"மிளகு"});
        ALIAS_MAP.put("pepper", new String[]{"மிளகு"});
        ALIAS_MAP.put("milagai", new String[]{"மிளகாய்", "வத்தல்"});
        ALIAS_MAP.put("milagaai", new String[]{"மிளகாய்"});
        ALIAS_MAP.put("malli", new String[]{"மல்லி", "தனியா"});
        ALIAS_MAP.put("coriander", new String[]{"மல்லி", "தனியா"});
        ALIAS_MAP.put("dhaniya", new String[]{"மல்லி", "தனியா"});
        ALIAS_MAP.put("manjal", new String[]{"மஞ்சள்"});
        ALIAS_MAP.put("turmeric", new String[]{"மஞ்சள்"});
        ALIAS_MAP.put("salt", new String[]{"உப்பு"});
        ALIAS_MAP.put("uppu", new String[]{"உப்பு"});
        ALIAS_MAP.put("tea", new String[]{"டீ", "தேநீர்", "தேயிலை"});
        ALIAS_MAP.put("coffee", new String[]{"காபி"});
        ALIAS_MAP.put("milk", new String[]{"பால்", "மில்க்"});
        ALIAS_MAP.put("paal", new String[]{"பால்"});
        ALIAS_MAP.put("ghee", new String[]{"நெய்"});
        ALIAS_MAP.put("nei", new String[]{"நெய்"});
        ALIAS_MAP.put("butter", new String[]{"வெண்ணெய்"});
        ALIAS_MAP.put("vennai", new String[]{"வெண்ணெய்"});
        ALIAS_MAP.put("puli", new String[]{"புளி"});
        ALIAS_MAP.put("tamarind", new String[]{"புளி"});
        ALIAS_MAP.put("poondu", new String[]{"பூண்டு"});
        ALIAS_MAP.put("garlic", new String[]{"பூண்டு"});
        ALIAS_MAP.put("vengayam", new String[]{"வெங்காயம்"});
        ALIAS_MAP.put("onion", new String[]{"வெங்காயம்"});
        ALIAS_MAP.put("appalam", new String[]{"அப்பளம்"});
        ALIAS_MAP.put("pappad", new String[]{"அப்பளம்"});
        ALIAS_MAP.put("perungayam", new String[]{"பெருங்காயம்"});
        ALIAS_MAP.put("perungaayam", new String[]{"பெருங்காயம்"});
        ALIAS_MAP.put("hing", new String[]{"பெருங்காயம்"});
        ALIAS_MAP.put("sombu", new String[]{"சோம்பு"});
        ALIAS_MAP.put("fennel", new String[]{"சோம்பு"});
        ALIAS_MAP.put("venthayam", new String[]{"வெந்தயம்"});
        ALIAS_MAP.put("vendayam", new String[]{"வெந்தயம்"});
        ALIAS_MAP.put("fenugreek", new String[]{"வெந்தயம்"});
        ALIAS_MAP.put("elakkai", new String[]{"ஏலக்காய்"});
        ALIAS_MAP.put("cardamom", new String[]{"ஏலக்காய்"});
        ALIAS_MAP.put("pattai", new String[]{"பட்டை"});
        ALIAS_MAP.put("cinnamon", new String[]{"பட்டை"});
        ALIAS_MAP.put("grambu", new String[]{"கிராம்பு"});
        ALIAS_MAP.put("krambu", new String[]{"கிராம்பு"});
        ALIAS_MAP.put("clove", new String[]{"கிராம்பு"});
        ALIAS_MAP.put("mundhiri", new String[]{"முந்திரி"});
        ALIAS_MAP.put("cashew", new String[]{"முந்திரி"});
        ALIAS_MAP.put("draksha", new String[]{"திராட்சை"});
        ALIAS_MAP.put("kismis", new String[]{"திராட்சை"});

        // Flours & Grains
        ALIAS_MAP.put("atta", new String[]{"ஆட்டா", "கோதுமை"});
        ALIAS_MAP.put("wheat", new String[]{"கோதுமை", "ஆட்டா"});
        ALIAS_MAP.put("godhumai", new String[]{"கோதுமை", "ஆட்டா"});
        ALIAS_MAP.put("maida", new String[]{"மைதா"});
        ALIAS_MAP.put("rava", new String[]{"ரவா", "ரவை"});
        ALIAS_MAP.put("ravai", new String[]{"ரவா", "ரவை"});
        ALIAS_MAP.put("sooji", new String[]{"ரவா", "ரவை"});
        ALIAS_MAP.put("rice", new String[]{"அரிசி"});
        ALIAS_MAP.put("arisi", new String[]{"அரிசி"});
        ALIAS_MAP.put("pasiarisi", new String[]{"பச்சரிசி"});
        ALIAS_MAP.put("puzhungal", new String[]{"புழுங்கல்"});

        // Personal care & Snacks
        ALIAS_MAP.put("soap", new String[]{"சோப்", "சோப்பு"});
        ALIAS_MAP.put("shampoo", new String[]{"ஷாம்பு"});
        ALIAS_MAP.put("paste", new String[]{"பேஸ்ட்"});
        ALIAS_MAP.put("brush", new String[]{"பிரஷ்"});
        ALIAS_MAP.put("biscuit", new String[]{"பிஸ்கட்", "பிஸ்கெட்"});
        ALIAS_MAP.put("biscut", new String[]{"பிஸ்கட்", "பிஸ்கெட்"});
        ALIAS_MAP.put("marie", new String[]{"மேரி"});
        ALIAS_MAP.put("rusk", new String[]{"ரஸ்க்"});
        ALIAS_MAP.put("maggi", new String[]{"மேகி", "நூடூல்ஸ்"});
        ALIAS_MAP.put("noodles", new String[]{"நூடூல்ஸ்", "மேகி"});

        // Units & Packages
        ALIAS_MAP.put("packet", new String[]{"பாக்கெட்", "packet"});
        ALIAS_MAP.put("pkt", new String[]{"பாக்கெட்", "packet"});
        ALIAS_MAP.put("box", new String[]{"பாக்ஸ்", "box"});
        ALIAS_MAP.put("bottle", new String[]{"பாட்டில்", "bottle"});
        ALIAS_MAP.put("btl", new String[]{"பாட்டில்", "bottle"});
        ALIAS_MAP.put("piece", new String[]{"பீஸ்", "piece"});
        ALIAS_MAP.put("pcs", new String[]{"பீஸ்", "piece"});
        ALIAS_MAP.put("pc", new String[]{"பீஸ்", "piece"});
        ALIAS_MAP.put("saram", new String[]{"சரம்", "saram"});
        ALIAS_MAP.put("pandal", new String[]{"பந்தல்", "பண்டல்", "pandal"});
        ALIAS_MAP.put("dozen", new String[]{"டஜன்", "dozen"});
        ALIAS_MAP.put("doz", new String[]{"டஜன்", "dozen"});
        ALIAS_MAP.put("kg", new String[]{"கிலோ", "kg"});
        ALIAS_MAP.put("kilo", new String[]{"கிலோ", "kg"});
        ALIAS_MAP.put("gram", new String[]{"கிராம்", "g"});
        ALIAS_MAP.put("gm", new String[]{"கிராம்", "g"});
        ALIAS_MAP.put("gms", new String[]{"கிராம்", "g"});
        ALIAS_MAP.put("liter", new String[]{"லிட்டர்", "லி", "l"});
        ALIAS_MAP.put("litre", new String[]{"லிட்டர்", "லி", "l"});
        ALIAS_MAP.put("ltr", new String[]{"லிட்டர்", "லி", "l"});
        ALIAS_MAP.put("li", new String[]{"லி", "லிட்டர்", "l"});
        ALIAS_MAP.put("ml", new String[]{"மில்லி", "ml"});

        // Fractions & Quantities
        ALIAS_MAP.put("1/2", new String[]{"1/2லி", "1/2", "500ml", "500g", "அரை"});
        ALIAS_MAP.put("1/4", new String[]{"1/4லி", "1/4", "250ml", "250g", "கால்"});
        ALIAS_MAP.put("3/4", new String[]{"3/4லி", "3/4", "750ml", "750g", "முக்கால்"});
        ALIAS_MAP.put("500", new String[]{"1/2லி", "500ml", "500g", "1/2"});
        ALIAS_MAP.put("500ml", new String[]{"1/2லி", "500ml", "1/2"});
        ALIAS_MAP.put("250", new String[]{"1/4லி", "250ml", "250g", "1/4"});
        ALIAS_MAP.put("250ml", new String[]{"1/4லி", "250ml", "1/4"});
        ALIAS_MAP.put("100", new String[]{"100ml", "100g", "100"});
        ALIAS_MAP.put("100ml", new String[]{"100ml", "100"});
        ALIAS_MAP.put("1l", new String[]{"1லி", "1l", "1ltr", "1000ml"});
        ALIAS_MAP.put("1ltr", new String[]{"1லி", "1l", "1ltr", "1000ml"});
        ALIAS_MAP.put("1kg", new String[]{"1கிலோ", "1kg", "1000g"});
    }

    public static String normalizeText(String input) {
        if (input == null) return "";
        return input.toLowerCase()
                .replace("&", " ")
                .replace("/", " ")
                .replace("(", " ")
                .replace(")", " ")
                .replace("-", " ")
                .replace(".", " ")
                .replace(",", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public static String extractCoreBrandName(String text) {
        if (text == null) return "";
        // Remove price prefixes like "Rs.10", "Rs. 10", "Rs." and unit suffixes in brackets like "(PIECE)", "(SARAM)", "(DOZEN)"
        return text.replaceAll("(?i)rs\\.?\\s*\\d+", "")
                   .replaceAll("\\(.*?\\)", "")
                   .replaceAll("\\s+", " ")
                   .trim();
    }

    public static List<String> expandTokenVariants(String token) {
        List<String> list = new ArrayList<>();
        if (token == null || token.trim().isEmpty()) return list;
        String t = token.trim().toLowerCase();
        list.add(t);

        String norm = normalizeText(t);
        if (!norm.isEmpty() && !norm.equals(t)) {
            list.add(norm);
        }

        if (ALIAS_MAP.containsKey(t)) {
            for (String alias : ALIAS_MAP.get(t)) {
                list.add(alias.toLowerCase());
            }
        }
        if (ALIAS_MAP.containsKey(norm)) {
            for (String alias : ALIAS_MAP.get(norm)) {
                list.add(alias.toLowerCase());
            }
        }

        // Bidirectional lookup: If token matches any alias value, also add the key and peer aliases
        for (Map.Entry<String, String[]> entry : ALIAS_MAP.entrySet()) {
            for (String alias : entry.getValue()) {
                if (alias.equalsIgnoreCase(t) || alias.equalsIgnoreCase(norm)) {
                    list.add(entry.getKey().toLowerCase());
                    for (String peer : entry.getValue()) {
                        list.add(peer.toLowerCase());
                    }
                    break;
                }
            }
        }

        if (t.contains("li") || t.contains("ltr")) {
            list.add(t.replace("li", "லி").replace("ltr", "லி"));
        }
        if (t.endsWith("kg")) {
            list.add(t.replace("kg", "கிலோ"));
        }

        return list;
    }

    public static boolean matchesTamilPhoneticPrefix(String word, String prefix) {
        if (word == null || prefix == null || prefix.isEmpty()) return false;
        
        // Phonetic prefix matching ONLY applies to single character prefix e.g. "s" -> "ச"
        if (prefix.length() != 1 || prefix.matches(".*\\d.*")) {
            return false;
        }

        // Must be a Tamil word (contains Tamil Unicode characters)
        boolean hasTamil = false;
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            if (c >= '\u0B80' && c <= '\u0BFF') {
                hasTamil = true;
                break;
            }
        }
        if (!hasTamil) return false;

        char firstChar = Character.toLowerCase(prefix.charAt(0));

        switch (firstChar) {
            case 'p':
            case 'b':
                return word.startsWith("ப");
            case 'r':
                return word.startsWith("ர");
            case 's':
            case 'c':
                return word.startsWith("ச") || word.startsWith("ஸ") || word.startsWith("ஶ்");
            case 'v':
            case 'w':
                return word.startsWith("வ");
            case 'a':
                return word.startsWith("அ") || word.startsWith("ஆ");
            case 'e':
                return word.startsWith("எ") || word.startsWith("ஏ") || word.startsWith("ஐ");
            case 'i':
                return word.startsWith("இ") || word.startsWith("ஈ");
            case 'o':
                return word.startsWith("ஒ") || word.startsWith("ஓ") || word.startsWith("ஔ");
            case 'u':
                return word.startsWith("உ") || word.startsWith("ஊ");
            case 'k':
            case 'g':
                return word.startsWith("க");
            case 't':
            case 'd':
                return word.startsWith("த") || word.startsWith("ட");
            case 'n':
                return word.startsWith("ந") || word.startsWith("ன") || word.startsWith("ண");
            case 'm':
                return word.startsWith("ம");
            case 'l':
                return word.startsWith("ல") || word.startsWith("ள") || word.startsWith("ழ");
            case 'y':
            case 'j':
                return word.startsWith("ய") || word.startsWith("ஜ");
            case 'h':
                return word.startsWith("ஹ");
            default:
                return false;
        }
    }

    public static boolean matchesWordStart(String text, String token) {
        if (text == null || token == null || token.isEmpty()) return false;
        String tLower = token.toLowerCase();

        String[] words = text.toLowerCase().split("[\\s+&/()\\-.,_]+");
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (word.startsWith(tLower)) {
                return true;
            }
            if (tLower.length() == 1 && matchesTamilPhoneticPrefix(word, tLower)) {
                return true;
            }
        }
        return false;
    }

    public static boolean matchesRemainder(Product p, String rem) {
        if (p == null || rem == null || rem.trim().isEmpty()) return false;
        String r = rem.trim().toLowerCase();

        String rawName = p.getName() != null ? p.getName().trim() : "";
        String rawDisplay = p.getDisplayName() != null ? p.getDisplayName().trim() : "";
        String rawTamil = p.getTamilName() != null ? p.getTamilName().trim() : "";
        String rawEnglish = p.getEnglishName() != null ? p.getEnglishName().trim() : "";

        String coreName = extractCoreBrandName(rawName).toLowerCase();
        String coreDisplay = extractCoreBrandName(rawDisplay).toLowerCase();
        String coreTamil = extractCoreBrandName(rawTamil).toLowerCase();
        String coreEnglish = extractCoreBrandName(rawEnglish).toLowerCase();

        if ((!coreName.isEmpty() && coreName.startsWith(r)) ||
            (!coreDisplay.isEmpty() && coreDisplay.startsWith(r)) ||
            (!coreTamil.isEmpty() && coreTamil.startsWith(r)) ||
            (!coreEnglish.isEmpty() && coreEnglish.startsWith(r))) {
            return true;
        }

        if (matchesWordStart(rawName, r) || matchesWordStart(rawDisplay, r) ||
            matchesWordStart(rawTamil, r) || matchesWordStart(rawEnglish, r) ||
            matchesWordStart(coreName, r) || matchesWordStart(coreTamil, r)) {
            return true;
        }

        List<String> variants = expandTokenVariants(r);
        for (String v : variants) {
            if ((!coreTamil.isEmpty() && coreTamil.startsWith(v)) ||
                (!coreName.isEmpty() && coreName.startsWith(v)) ||
                matchesWordStart(rawName, v) || matchesWordStart(rawTamil, v) ||
                matchesWordStart(rawDisplay, v)) {
                return true;
            }
        }

        return false;
    }

    public static class ParsedQuery {
        public String codePart = null;
        public String itemPart = null;
        public List<String> tokens = new ArrayList<>();
    }

    public static ParsedQuery parseQuery(String query, List<Product> allProducts) {
        ParsedQuery pq = new ParsedQuery();
        if (query == null || query.trim().isEmpty()) return pq;
        String q = query.trim().toLowerCase();

        Set<String> knownBarcodes = new HashSet<>();
        if (allProducts != null) {
            for (Product p : allProducts) {
                if (p.getBarcode() != null && !p.getBarcode().trim().isEmpty()) {
                    knownBarcodes.add(p.getBarcode().trim().toLowerCase());
                }
            }
        }
        List<String> sortedBarcodes = new ArrayList<>(knownBarcodes);
        sortedBarcodes.sort((a, b) -> Integer.compare(b.length(), a.length()));

        String[] rawTokens = q.split("\\s+");
        for (String t : rawTokens) {
            if (knownBarcodes.contains(t)) {
                pq.tokens.add(t);
                pq.codePart = t;
                continue;
            }

            boolean split = false;

            // Pattern 1: Code (letter+digit) followed immediately by item spelling: e.g. b10mar, b5tig, s10lux, tc10tata
            Matcher m = Pattern.compile("^([a-z]+[0-9]+(?:\\.[0-9]+)?)([a-z\\u0B80-\\u0BFF].*)$").matcher(t);
            if (m.matches()) {
                String c = m.group(1);
                String it = m.group(2);
                pq.codePart = c;
                pq.itemPart = it;
                pq.tokens.add(c);
                pq.tokens.add(it);
                split = true;
            }

            // Pattern 2: Known barcode with digits + remaining item (including numbers like 50 for 50-50)
            if (!split) {
                for (String bc : sortedBarcodes) {
                    if (bc.matches(".*\\d.*") && t.startsWith(bc) && t.length() > bc.length()) {
                        String rem = t.substring(bc.length()).trim();
                        if (!rem.isEmpty()) {
                            pq.codePart = bc;
                            pq.itemPart = rem;
                            pq.tokens.add(bc);
                            pq.tokens.add(rem);
                            split = true;
                            break;
                        }
                    }
                }
            }

            // Pattern 3: Any known barcode (letters only, length >= 1) followed by item spelling matching a product with that barcode
            if (!split) {
                for (String bc : sortedBarcodes) {
                    if (t.startsWith(bc) && t.length() > bc.length()) {
                        String rem = t.substring(bc.length()).trim();
                        if (!rem.isEmpty() && !knownBarcodes.contains(t)) {
                            boolean prodMatches = false;
                            if (allProducts != null) {
                                for (Product p : allProducts) {
                                    if (p.getBarcode() != null && p.getBarcode().trim().equalsIgnoreCase(bc)) {
                                        if (matchesRemainder(p, rem)) {
                                            prodMatches = true;
                                            break;
                                        }
                                    }
                                }
                            }
                            if (prodMatches) {
                                pq.codePart = bc;
                                pq.itemPart = rem;
                                pq.tokens.add(bc);
                                pq.tokens.add(rem);
                                split = true;
                                break;
                            }
                        }
                    }
                }
            }

            if (!split) {
                pq.tokens.add(t);
                if (knownBarcodes.contains(t)) {
                    pq.codePart = t;
                }
            }
        }
        return pq;
    }

    private static boolean matchTokenAgainstProduct(Product p, String token) {
        if (token == null || token.isEmpty()) return false;
        String t = token.toLowerCase();

        String code = p.getBarcode() != null ? p.getBarcode().trim().toLowerCase() : "";
        String rawName = p.getName() != null ? p.getName().trim() : "";
        String rawDisplay = p.getDisplayName() != null ? p.getDisplayName().trim() : "";
        String rawTamil = p.getTamilName() != null ? p.getTamilName().trim() : "";
        String rawEnglish = p.getEnglishName() != null ? p.getEnglishName().trim() : "";
        String unit = p.getUnitType() != null ? p.getUnitType().trim().toLowerCase() : "";
        String category = p.getCategory() != null ? p.getCategory().trim().toLowerCase() : "";

        // 1. Code match (exact or prefix)
        if (!code.isEmpty()) {
            if (code.equals(t) || code.startsWith(t)) {
                return true;
            }
        }

        // 2. Unit match (e.g. piece, saram, pandal, dozen)
        if (!unit.isEmpty()) {
            if (unit.equals(t)) return true;
            if (unit.startsWith(t) && t.length() >= 2) return true;
        }

        // 3. Core brand name starts with or words start with token (Left-to-Right match)
        String coreName = extractCoreBrandName(rawName).toLowerCase();
        String coreTamil = extractCoreBrandName(rawTamil).toLowerCase();
        String coreDisplay = extractCoreBrandName(rawDisplay).toLowerCase();
        String coreEnglish = extractCoreBrandName(rawEnglish).toLowerCase();
        if (coreName.startsWith(t) || coreTamil.startsWith(t) || coreDisplay.startsWith(t) || coreEnglish.startsWith(t)) {
            return true;
        }
        if (matchesWordStart(coreName, t) || matchesWordStart(coreTamil, t)) {
            return true;
        }

        // 4. Word start in English name or Name
        if (matchesWordStart(rawEnglish, t) || matchesWordStart(rawName, t)) {
            return true;
        }

        // 5. Word start in Tamil display or Tamil print name
        if (matchesWordStart(rawDisplay, t) || matchesWordStart(rawTamil, t)) {
            return true;
        }

        // 6. Check alias expansions (e.g. "sambar" -> "சாம்பார்", "seeni" -> "சீனி")
        List<String> variants = expandTokenVariants(t);
        String allText = (rawEnglish + " " + rawName + " " + rawDisplay + " " + rawTamil + " " + unit + " " + category + " " + code).toLowerCase();
        String normAllText = normalizeText(allText);

        for (String v : variants) {
            if (v.length() >= 2 && (allText.contains(v) || normAllText.contains(v))) {
                return true;
            }
        }

        // 7. Substring match for tokens >= 2 chars
        if (t.length() >= 2) {
            if (rawEnglish.toLowerCase().contains(t) || rawName.toLowerCase().contains(t) ||
                rawDisplay.toLowerCase().contains(t) || rawTamil.toLowerCase().contains(t) ||
                category.contains(t)) {
                return true;
            }
        }

        // 8. Single letter phonetic match for Tamil words (ONLY if token length == 1)
        if (t.length() == 1) {
            String[] tamWords = (rawDisplay + " " + rawTamil).toLowerCase().split("[\\s+&/()\\-.,_]+");
            for (String w : tamWords) {
                if (matchesTamilPhoneticPrefix(w, t)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static List<Product> searchProducts(List<Product> allProducts, String query, int limit) {
        List<Product> result = new ArrayList<>();
        if (query == null || query.trim().isEmpty() || allProducts == null || allProducts.isEmpty()) {
            return result;
        }

        String rawQuery = query.trim();
        String rawQueryLower = rawQuery.toLowerCase();

        ParsedQuery pq = parseQuery(rawQuery, allProducts);
        List<String> tokens = pq.tokens;
        if (tokens.isEmpty()) return result;

        class ScoredProd {
            final Product product;
            final int score;
            ScoredProd(Product p, int s) { this.product = p; this.score = s; }
        }

        List<ScoredProd> scoredList = new ArrayList<>();

        for (Product p : allProducts) {
            String code = p.getBarcode() != null ? p.getBarcode().trim().toLowerCase() : "";
            String rawName = p.getName() != null ? p.getName().trim() : "";
            String rawDisplay = p.getDisplayName() != null ? p.getDisplayName().trim() : "";
            String rawTamil = p.getTamilName() != null ? p.getTamilName().trim() : "";
            String rawEnglish = p.getEnglishName() != null ? p.getEnglishName().trim() : "";
            String unit = p.getUnitType() != null ? p.getUnitType().trim().toLowerCase() : "";
            String category = p.getCategory() != null ? p.getCategory().trim().toLowerCase() : "";

            String coreName = extractCoreBrandName(rawName).toLowerCase();
            String coreDisplay = extractCoreBrandName(rawDisplay).toLowerCase();
            String coreTamil = extractCoreBrandName(rawTamil).toLowerCase();
            String coreEnglish = extractCoreBrandName(rawEnglish).toLowerCase();

            boolean allMatched = true;
            int totalScore = 0;

            for (String token : tokens) {
                if (!matchTokenAgainstProduct(p, token)) {
                    allMatched = false;
                    break;
                }

                int tokenScore = 0;

                // 1. Code match (Left-to-right from index 0)
                if (!code.isEmpty()) {
                    if (code.equals(token)) {
                        tokenScore = Math.max(tokenScore, 20000);
                    } else if (code.startsWith(token)) {
                        tokenScore = Math.max(tokenScore, 12000);
                    }
                }

                // 2. Core brand name starts with token (Left-to-right from index 0)
                if ((!coreName.isEmpty() && coreName.startsWith(token)) ||
                    (!coreDisplay.isEmpty() && coreDisplay.startsWith(token)) ||
                    (!coreTamil.isEmpty() && coreTamil.startsWith(token)) ||
                    (!coreEnglish.isEmpty() && coreEnglish.startsWith(token))) {
                    tokenScore = Math.max(tokenScore, 16000);
                }

                // 3. Raw names start with token (e.g. "rs.10 ...")
                if (rawName.toLowerCase().startsWith(token) || rawDisplay.toLowerCase().startsWith(token) ||
                    rawTamil.toLowerCase().startsWith(token) || rawEnglish.toLowerCase().startsWith(token)) {
                    tokenScore = Math.max(tokenScore, 14000);
                }

                // 4. Word start in Core Brand / Name (Left to Right by word position)
                String[] words = (coreName + " " + coreTamil + " " + rawName + " " + rawTamil).split("[\\s+&/()\\-.,_]+");
                for (int wIdx = 0; wIdx < words.length; wIdx++) {
                    String w = words[wIdx];
                    if (w.isEmpty()) continue;
                    if (w.startsWith(token)) {
                        int posBonus = Math.max(1000, 8000 - (wIdx * 1000));
                        tokenScore = Math.max(tokenScore, posBonus);
                    } else if (token.length() == 1 && matchesTamilPhoneticPrefix(w, token)) {
                        tokenScore = Math.max(tokenScore, Math.max(500, 5000 - (wIdx * 500)));
                    }
                }

                // 5. Unit match
                if (!unit.isEmpty()) {
                    if (unit.equals(token)) {
                        tokenScore = Math.max(tokenScore, 4000);
                    } else if (unit.startsWith(token) && token.length() >= 2) {
                        tokenScore = Math.max(tokenScore, 3000);
                    }
                }

                // 6. Check aliases (e.g. "sambar", "seeni", "oil")
                List<String> variants = expandTokenVariants(token);
                for (String v : variants) {
                    if (coreTamil.startsWith(v) || coreName.startsWith(v)) {
                        tokenScore = Math.max(tokenScore, 10000);
                        break;
                    } else if (coreTamil.contains(v) || coreName.contains(v) || rawTamil.contains(v) || rawName.toLowerCase().contains(v)) {
                        tokenScore = Math.max(tokenScore, 5000);
                        break;
                    }
                }

                // 7. Substring contains match (length >= 2)
                if (token.length() >= 2) {
                    if (coreName.contains(token) || coreTamil.contains(token) ||
                        rawName.toLowerCase().contains(token) || rawTamil.toLowerCase().contains(token) ||
                        category.contains(token)) {
                        tokenScore = Math.max(tokenScore, 2000);
                    }
                }

                totalScore += tokenScore;
            }

            // Fallback for barcode prefix match
            if (!allMatched && !code.isEmpty() && rawQueryLower.startsWith(code) && rawQueryLower.length() > code.length()) {
                String rem = rawQueryLower.substring(code.length()).trim();
                if (matchesRemainder(p, rem)) {
                    allMatched = true;
                    totalScore += 25000;
                }
            }

            if (!allMatched) continue;

            // Extra bonus if code AND item both matched (e.g. b10 + mar, h + ku)
            if (pq.codePart != null && pq.itemPart != null) {
                if (code.equals(pq.codePart) && (coreName.startsWith(pq.itemPart) || coreTamil.startsWith(pq.itemPart) ||
                    rawName.toLowerCase().contains(pq.itemPart) || rawTamil.toLowerCase().contains(pq.itemPart) ||
                    matchesRemainder(p, pq.itemPart))) {
                    totalScore += 30000; // Super priority for code + item match!
                }
            }

            // Exact full query bonuses
            if (code.equalsIgnoreCase(rawQueryLower)) {
                totalScore += 25000;
            } else if (coreName.equalsIgnoreCase(rawQueryLower) || coreTamil.equalsIgnoreCase(rawQueryLower) || rawDisplay.equalsIgnoreCase(rawQueryLower) || rawTamil.equalsIgnoreCase(rawQueryLower) || rawName.equalsIgnoreCase(rawQueryLower) || rawEnglish.equalsIgnoreCase(rawQueryLower)) {
                totalScore += 20000;
            } else if (coreName.startsWith(rawQueryLower) || coreTamil.startsWith(rawQueryLower) || rawDisplay.toLowerCase().startsWith(rawQueryLower) || rawTamil.toLowerCase().startsWith(rawQueryLower) || rawName.toLowerCase().startsWith(rawQueryLower) || rawEnglish.toLowerCase().startsWith(rawQueryLower)) {
                totalScore += 12000;
            }

            scoredList.add(new ScoredProd(p, totalScore));
        }

        // Sort by score descending; on tie, prefer lower price / original ID order
        scoredList.sort((a, b) -> {
            if (b.score != a.score) return Integer.compare(b.score, a.score);
            return Integer.compare(a.product.getId(), b.product.getId());
        });

        int count = 0;
        for (ScoredProd sp : scoredList) {
            result.add(sp.product);
            count++;
            if (limit > 0 && count >= limit) break;
        }

        return result;
    }
}
