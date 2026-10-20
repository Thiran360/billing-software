package com.store.billing.util;

import com.store.billing.model.Customer;
import java.util.*;

public class CustomerSearchUtil {

    private static final String[] TITLES = {
        "திருமதி.", "திருமதி", "திரு.", "திரு", "செல்வி.", "செல்வி", "டாக்டர்.", "டாக்டர்", "Mr.", "Mr", "Mrs.", "Mrs", "Miss", "Dr.", "Dr"
    };

    public static String stripTitle(String name) {
        if (name == null) return "";
        String trimmed = name.trim();
        for (String title : TITLES) {
            if (trimmed.startsWith(title)) {
                return trimmed.substring(title.length()).trim();
            }
        }
        return trimmed;
    }

    private static final Map<String, String[]> NAME_MAP = new HashMap<>();
    static {
        NAME_MAP.put("babu", new String[]{"பாபு"});
        NAME_MAP.put("karthi", new String[]{"கார்த்தி", "கார்த்திக்"});
        NAME_MAP.put("karthik", new String[]{"கார்த்திக்", "கார்த்தி"});
        NAME_MAP.put("muthu", new String[]{"முத்து"});
        NAME_MAP.put("raja", new String[]{"ராஜா", "ராஜன்"});
        NAME_MAP.put("rajan", new String[]{"ராஜன்", "ராஜா"});
        NAME_MAP.put("senthil", new String[]{"செந்தில்"});
        NAME_MAP.put("mani", new String[]{"மணி"});
        NAME_MAP.put("kumar", new String[]{"குமார்", "குமரன்"});
        NAME_MAP.put("murugan", new String[]{"முருகன்"});
        NAME_MAP.put("suresh", new String[]{"சுரேஷ்"});
        NAME_MAP.put("saravanan", new String[]{"சரவணன்"});
        NAME_MAP.put("prakash", new String[]{"பிரகாஷ்"});
        NAME_MAP.put("dinesh", new String[]{"தினேஷ்"});
        NAME_MAP.put("ravi", new String[]{"ரவி"});
        NAME_MAP.put("mohan", new String[]{"மோகன்"});
        NAME_MAP.put("anbu", new String[]{"அன்பு"});
        NAME_MAP.put("vijay", new String[]{"விஜய்"});
        NAME_MAP.put("gopal", new String[]{"கோபால்"});
        NAME_MAP.put("siva", new String[]{"சிவா", "சிவ"});
        NAME_MAP.put("shiva", new String[]{"சிவா", "சிவ"});
        NAME_MAP.put("velu", new String[]{"வேலு", "வேல்"});
        NAME_MAP.put("vel", new String[]{"வேல்", "வேலு"});
        NAME_MAP.put("nataraj", new String[]{"நடராஜ்", "நட்ராஜ்"});
        NAME_MAP.put("natraj", new String[]{"நட்ராஜ்", "நடராஜ்"});
        NAME_MAP.put("paramasivam", new String[]{"பரமசிவம்"});
        NAME_MAP.put("mariappan", new String[]{"மாரியப்பன்"});
        NAME_MAP.put("chidambaram", new String[]{"சிதம்பரம்"});
        NAME_MAP.put("kiruba", new String[]{"கிருபா"});
        NAME_MAP.put("mayil", new String[]{"மயில்"});
        NAME_MAP.put("papathi", new String[]{"பாபாத்தி"});
        NAME_MAP.put("balu", new String[]{"பாலு", "பாலா"});
        NAME_MAP.put("bala", new String[]{"பாலா", "பாலன்"});
        NAME_MAP.put("selvam", new String[]{"செல்வம்", "செல்வா"});
        NAME_MAP.put("selvan", new String[]{"செல்வன்"});
        NAME_MAP.put("kannan", new String[]{"கண்ணன்"});
        NAME_MAP.put("arun", new String[]{"அருண்"});
        NAME_MAP.put("vinoth", new String[]{"வினோத்"});
        NAME_MAP.put("govind", new String[]{"கோவிந்த", "கோவிந்தன்"});
        NAME_MAP.put("hari", new String[]{"ஹரி"});
        NAME_MAP.put("gopi", new String[]{"கோபி"});
        NAME_MAP.put("guna", new String[]{"குணா"});
        NAME_MAP.put("deva", new String[]{"தேவா"});
        NAME_MAP.put("sri", new String[]{"ஸ்ரீ"});
        NAME_MAP.put("shri", new String[]{"ஸ்ரீ"});
        NAME_MAP.put("chinnasamy", new String[]{"சின்னசாமி"});
        NAME_MAP.put("ramasamy", new String[]{"ராமசாமி"});
        NAME_MAP.put("palanisamy", new String[]{"பழனிசாமி"});
        NAME_MAP.put("krishnan", new String[]{"கிருஷ்ணன்"});
        NAME_MAP.put("subramani", new String[]{"சுப்பிரமணி", "சுப்ரமணி"});
        NAME_MAP.put("thangavel", new String[]{"தங்கவேல்"});
        NAME_MAP.put("sakthi", new String[]{"சக்தி"});
        NAME_MAP.put("shakti", new String[]{"சக்தி"});
        NAME_MAP.put("durai", new String[]{"துரை"});
        NAME_MAP.put("ramesh", new String[]{"ரமேஷ்"});
        NAME_MAP.put("anand", new String[]{"ஆனந்த்", "ஆனந்தன்"});
        NAME_MAP.put("anandan", new String[]{"ஆனந்தன்", "ஆனந்த்"});
        NAME_MAP.put("boopathi", new String[]{"பூபதி"});
        NAME_MAP.put("bupathi", new String[]{"பூபதி"});
        NAME_MAP.put("sasikumar", new String[]{"சசிகுமார்", "சசி"});
        NAME_MAP.put("sasi", new String[]{"சசி", "சசிகுமார்"});
        NAME_MAP.put("vadivel", new String[]{"வடிவேல்"});
        NAME_MAP.put("selvaraj", new String[]{"செல்வராஜ்"});
        NAME_MAP.put("marimuthu", new String[]{"மாரிமுத்து"});
        NAME_MAP.put("shanmugam", new String[]{"சண்முகம்"});
        NAME_MAP.put("vetri", new String[]{"வெற்றி"});
        NAME_MAP.put("lakshmi", new String[]{"லட்சுமி"});
        NAME_MAP.put("radha", new String[]{"ராதா"});
    }

    public static String transliterateTanglishToTamil(String input) {
        if (input == null || input.isEmpty()) return "";
        String s = input.toLowerCase().trim();

        String[][] table = {
            {"shri", "ஸ்ரீ"}, {"sri", "ஸ்ரீ"},
            {"karthik", "கார்த்திக்"}, {"karthi", "கார்த்தி"},
            {"kumar", "குமார்"}, {"murugan", "முருகன்"}, {"muthu", "முத்து"},
            {"babu", "பாபு"}, {"balu", "பாலு"}, {"bala", "பாலா"},
            {"mani", "மணி"}, {"raja", "ராஜா"}, {"rajan", "ராஜன்"},
            {"siva", "சிவா"}, {"shiva", "சிவா"}, {"ravi", "ரவி"},
            {"dinesh", "தினேஷ்"}, {"suresh", "சுரேஷ்"},
            {"selvam", "செல்வம்"}, {"selvan", "செல்வன்"},
            {"gopal", "கோபால்"}, {"gopi", "கோபி"}, {"guna", "குணா"},
            {"anbu", "அன்பு"}, {"arun", "அருண்"}, {"vijay", "விஜய்"},
            // Syllables
            {"ka", "கா"}, {"ki", "கி"}, {"ku", "கு"}, {"ke", "கே"}, {"ko", "கோ"},
            {"ga", "க"}, {"gi", "கி"}, {"gu", "கு"}, {"ge", "கே"}, {"go", "கோ"},
            {"sa", "சா"}, {"si", "சி"}, {"su", "சு"}, {"se", "செ"}, {"so", "சோ"},
            {"cha", "சா"}, {"chi", "சி"}, {"chu", "சு"}, {"che", "செ"}, {"cho", "சோ"},
            {"ta", "தா"}, {"ti", "தி"}, {"tu", "து"}, {"te", "தே"}, {"to", "தோ"},
            {"tha", "தா"}, {"thi", "தி"}, {"thu", "து"}, {"the", "தே"}, {"tho", "தோ"},
            {"da", "ட"}, {"di", "தி"}, {"du", "து"}, {"de", "தே"}, {"do", "தோ"},
            {"na", "நா"}, {"ni", "னி"}, {"nu", "னு"}, {"ne", "நே"}, {"no", "நோ"},
            {"pa", "பா"}, {"pi", "பி"}, {"pu", "பு"}, {"pe", "பே"}, {"po", "போ"},
            {"ba", "பா"}, {"bi", "பி"}, {"bu", "பு"}, {"be", "பே"}, {"bo", "போ"},
            {"ma", "மா"}, {"mi", "மி"}, {"mu", "மு"}, {"me", "மே"}, {"mo", "மோ"},
            {"ya", "யா"}, {"yi", "யி"}, {"yu", "யு"}, {"ye", "யே"}, {"yo", "யோ"},
            {"ra", "ரா"}, {"ri", "ரி"}, {"ru", "ரு"}, {"re", "ரே"}, {"ro", "ரோ"},
            {"la", "லா"}, {"li", "லி"}, {"lu", "லு"}, {"le", "லே"}, {"lo", "லோ"},
            {"va", "வா"}, {"vi", "வி"}, {"vu", "வு"}, {"ve", "வே"}, {"vo", "வோ"},
            {"ja", "ஜா"}, {"ji", "ஜி"}, {"ju", "ஜு"}, {"je", "ஜே"}, {"jo", "ஜோ"},
            {"ha", "ஹா"}, {"hi", "ஹி"}, {"hu", "ஹு"}, {"he", "ஹே"}, {"ho", "ஹோ"},
            // Single letters
            {"k", "க்"}, {"g", "க்"}, {"s", "ஸ்"}, {"c", "ச்"}, {"t", "த்"},
            {"d", "ட்"}, {"p", "ப்"}, {"b", "ப்"}, {"m", "ம்"}, {"n", "ன்"},
            {"r", "ர்"}, {"l", "ல்"}, {"v", "வ்"}, {"y", "ய்"}, {"j", "ஜ்"},
            {"a", "அ"}, {"e", "எ"}, {"i", "இ"}, {"o", "ஒ"}, {"u", "உ"}
        };

        StringBuilder sb = new StringBuilder();
        int idx = 0;
        while (idx < s.length()) {
            boolean matched = false;
            for (String[] pair : table) {
                if (s.startsWith(pair[0], idx)) {
                    sb.append(pair[1]);
                    idx += pair[0].length();
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                sb.append(s.charAt(idx));
                idx++;
            }
        }
        return sb.toString();
    }

    public static List<String> getTamilVariants(String query) {
        List<String> variants = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) return variants;
        String q = query.trim().toLowerCase();

        if (NAME_MAP.containsKey(q)) {
            variants.addAll(Arrays.asList(NAME_MAP.get(q)));
        }

        for (Map.Entry<String, String[]> entry : NAME_MAP.entrySet()) {
            if (q.startsWith(entry.getKey()) || (q.length() >= 3 && entry.getKey().startsWith(q))) {
                for (String v : entry.getValue()) {
                    if (!variants.contains(v)) variants.add(v);
                }
            }
        }

        String s = transliterateTanglishToTamil(q);
        if (s != null && !s.isEmpty() && !variants.contains(s)) {
            variants.add(s);
        }

        // Short prefixes (e.g. ba, pa, ka, ma)
        if (q.equals("ba") || q.equals("pa")) {
            if (!variants.contains("பா")) variants.add("பா");
            if (!variants.contains("ப")) variants.add("ப");
        } else if (q.equals("ka") || q.equals("ga")) {
            if (!variants.contains("கா")) variants.add("கா");
            if (!variants.contains("க")) variants.add("க");
        } else if (q.equals("ma")) {
            if (!variants.contains("மா")) variants.add("மா");
            if (!variants.contains("ம")) variants.add("ம");
        } else if (q.equals("ra")) {
            if (!variants.contains("ரா")) variants.add("ரா");
            if (!variants.contains("ர")) variants.add("ர");
        } else if (q.equals("sa")) {
            if (!variants.contains("சா")) variants.add("சா");
            if (!variants.contains("ச")) variants.add("ச");
        }

        return variants;
    }

    public static int calculateScore(Customer c, String query) {
        if (c == null) return 0;
        if (query == null || query.trim().isEmpty()) return 1000;

        String q = query.trim().toLowerCase();
        String idStr = String.valueOf(c.getId());
        String name = c.getName() != null ? c.getName().trim().toLowerCase() : "";
        String tamil = c.getTamilName() != null ? c.getTamilName().trim() : "";
        String mobile = c.getMobile() != null ? c.getMobile().trim() : "";
        String address = c.getAddress() != null ? c.getAddress().trim().toLowerCase() : "";

        String cleanTamil = stripTitle(tamil);
        String cleanEnglish = stripTitle(name);

        String[] tamilWords = cleanTamil.split("[\\s+.,_\\-]+");
        String firstTamilWord = tamilWords.length > 0 ? tamilWords[0] : "";

        String[] engWords = cleanEnglish.split("[\\s+.,_\\-]+");
        String firstEngWord = engWords.length > 0 ? engWords[0] : "";

        int score = 0;

        // 1. Direct ID match
        if (idStr.equals(q)) {
            return 50000;
        } else if (idStr.startsWith(q)) {
            score = Math.max(score, 35000);
        }

        // 2. Mobile match
        if (!mobile.isEmpty()) {
            if (mobile.equals(q)) {
                return 45000;
            } else if (mobile.startsWith(q)) {
                score = Math.max(score, 32000);
            } else if (mobile.contains(q)) {
                score = Math.max(score, 20000);
            }
        }

        // 3. English Name match
        if (!name.isEmpty()) {
            if (name.equalsIgnoreCase(q) || cleanEnglish.equalsIgnoreCase(q)) {
                score = Math.max(score, 45000);
            } else if (firstEngWord.equalsIgnoreCase(q)) {
                score = Math.max(score, 42000);
            } else if (firstEngWord.startsWith(q)) {
                score = Math.max(score, 32000);
            } else if (cleanEnglish.startsWith(q)) {
                score = Math.max(score, 28000);
            } else if (name.contains(q)) {
                score = Math.max(score, 18000);
            }
        }

        // 4. Tamil Direct Match (when user types Tamil letters)
        if (!tamil.isEmpty()) {
            if (cleanTamil.equals(q)) {
                score = Math.max(score, 45000);
            } else if (firstTamilWord.equals(q)) {
                score = Math.max(score, 42000);
            } else if (firstTamilWord.startsWith(q)) {
                score = Math.max(score, 35000);
            } else if (cleanTamil.startsWith(q)) {
                score = Math.max(score, 30000);
            } else if (cleanTamil.contains(q)) {
                score = Math.max(score, 18000);
            }
        }

        // 5. Transliteration / Phonetic match
        List<String> tamilVariants = getTamilVariants(q);
        for (String tv : tamilVariants) {
            if (tv.isEmpty()) continue;
            if (firstTamilWord.equals(tv)) {
                score = Math.max(score, 40000); // Exact first name match!
            } else if (firstTamilWord.startsWith(tv)) {
                score = Math.max(score, 35000); // First name starts with!
            } else if (cleanTamil.startsWith(tv)) {
                score = Math.max(score, 30000);
            } else {
                for (int w = 1; w < tamilWords.length; w++) {
                    if (tv.length() > 1 && tamilWords[w].startsWith(tv)) {
                        score = Math.max(score, 20000); // Town/Area match
                        break;
                    }
                }
            }
        }

        // 6. Address match
        if (!address.isEmpty() && address.contains(q)) {
            score = Math.max(score, 15000);
        }

        return score;
    }

    public static boolean matches(Customer c, String query) {
        return calculateScore(c, query) > 0;
    }

    public static List<Customer> filter(List<Customer> customers, String query) {
        List<Customer> result = new ArrayList<>();
        if (customers == null || customers.isEmpty()) return result;
        if (query == null || query.trim().isEmpty()) {
            result.addAll(customers);
            return result;
        }

        class ScoredCustomer {
            final Customer customer;
            final int score;
            ScoredCustomer(Customer c, int s) { this.customer = c; this.score = s; }
        }

        List<ScoredCustomer> scored = new ArrayList<>();
        for (Customer c : customers) {
            int s = calculateScore(c, query);
            if (s > 0) {
                scored.add(new ScoredCustomer(c, s));
            }
        }

        scored.sort((a, b) -> {
            if (b.score != a.score) return Integer.compare(b.score, a.score);
            return Integer.compare(a.customer.getId(), b.customer.getId());
        });

        for (ScoredCustomer sc : scored) {
            result.add(sc.customer);
        }
        return result;
    }

    public static String getDisplayLabel(Customer c) {
        if (c == null) return "";
        StringBuilder sb = new StringBuilder();
        String tamil = c.getTamilName() != null ? c.getTamilName().trim() : "";
        if (!tamil.isEmpty()) {
            sb.append(tamil);
        } else {
            sb.append(c.getName());
        }
        if (c.getMobile() != null && !c.getMobile().trim().isEmpty()) {
            sb.append(" | ").append(c.getMobile().trim());
        }
        return sb.toString();
    }
}
