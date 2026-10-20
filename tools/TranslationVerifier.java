import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;

/** Runs on the Gradle JDK; no Android device or third-party XML library required. */
class TranslationVerifier {
    private static final Set<String> INVARIANTS = Set.of("app_name", "symbol_increase", "symbol_decrease",
            "symbol_selected", "symbol_unselected", "time_separator", "time_unset");
    private static final Pattern ARGUMENT = Pattern.compile("%(?:(\\d+)\\$)?[-#+ 0,(]*\\d*(?:\\.\\d+)?([tT]?[a-zA-Z%])");
    private record Message(String type, boolean formatted, Map<String, String> forms) {}

    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("--self-test")) {
            selfTest();
            return;
        }
        verify(Path.of(args[0]));
    }

    private static Map<String, Message> catalog(Path folder, boolean canonical) throws Exception {
        Map<String, Message> result = new TreeMap<>();
        Set<String> seen = new HashSet<>();
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        try (var files = Files.list(folder)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".xml")).sorted().toList()) {
                var children = factory.newDocumentBuilder().parse(file.toFile()).getDocumentElement().getChildNodes();
                for (int i = 0; i < children.getLength(); i++) {
                    if (!(children.item(i) instanceof Element element)) continue;
                    String type = element.getTagName();
                    if (!Set.of("string", "plurals", "string-array").contains(type)) continue;
                    String key = element.getAttribute("name");
                    if (!seen.add(key)) fail(folder, key, "duplicate key");
                    if (element.getAttribute("translatable").equals("false")) {
                        if (!canonical || !INVARIANTS.contains(key)) fail(folder, key, "undocumented invariant");
                        continue;
                    }
                    Map<String, String> forms = new TreeMap<>();
                    if (type.equals("string")) {
                        forms.put("text", element.getTextContent());
                    } else {
                        var items = element.getElementsByTagName("item");
                        for (int j = 0; j < items.getLength(); j++) {
                            Element item = (Element) items.item(j);
                            String form = type.equals("plurals") ? item.getAttribute("quantity") : Integer.toString(j);
                            if (forms.put(form, item.getTextContent()) != null) fail(folder, key, "duplicate form " + form);
                        }
                    }
                    if (forms.isEmpty() || forms.values().stream().anyMatch(String::isBlank)) fail(folder, key, "empty message");
                    if (type.equals("plurals")) {
                        // These four catalogs require one/other; modern CLDR also uses many for es/pt/fr.
                        var required = folder.getFileName().toString().matches("values-(es|pt|fr)")
                                ? Set.of("one", "many", "other") : Set.of("one", "other");
                        if (!forms.keySet().containsAll(required)) fail(folder, key, "missing plural quantities " + required);
                        if (!Set.of("zero", "one", "two", "few", "many", "other").containsAll(forms.keySet()))
                            fail(folder, key, "unknown plural quantity");
                    }
                    boolean formatted = !element.getAttribute("formatted").equals("false");
                    if (formatted) for (String text : forms.values()) signature(folder, key, text);
                    result.put(key, new Message(type, formatted, forms));
                }
            }
        }
        return result;
    }

    private static Map<Integer, String> signature(Path folder, String key, String text) {
        var signature = new TreeMap<Integer, String>();
        var matcher = ARGUMENT.matcher(text);
        while (matcher.find()) {
            String conversion = matcher.group(2);
            if (conversion.equals("%") || conversion.equals("n")) continue;
            if (matcher.group(1) == null) fail(folder, key, "substitution must be indexed: " + matcher.group());
            int index = Integer.parseInt(matcher.group(1));
            String previous = signature.put(index, conversion);
            if (previous != null && !previous.equals(conversion)) fail(folder, key, "inconsistent argument " + index);
        }
        return signature;
    }

    static void verify(Path resources) throws Exception {
        var english = catalog(resources.resolve("values"), true);
        Set<String> languages = new TreeSet<>();
        try (var folders = Files.list(resources)) {
            for (Path folder : folders.filter(Files::isDirectory).sorted().toList()) {
                String name = folder.getFileName().toString();
                if (!name.matches("values-[a-z]{2,3}")) continue;
                languages.add(name.substring(7));
                var translated = catalog(folder, false);
                for (String key : english.keySet()) {
                    if (!translated.containsKey(key)) fail(folder, key, "missing translation");
                    var base = english.get(key);
                    var target = translated.get(key);
                    if (!base.type.equals(target.type) || base.formatted != target.formatted) fail(folder, key, "resource type mismatch");
                    if (!base.type.equals("plurals") && !base.forms.keySet().equals(target.forms.keySet()))
                        fail(folder, key, "array/form count mismatch");
                    for (var form : target.forms.entrySet()) {
                        String baseText = base.forms.getOrDefault(form.getKey(), base.forms.get("other"));
                        if (base.formatted && !signature(folder, key, baseText).equals(signature(folder, key, form.getValue())))
                            fail(folder, key, "placeholder mismatch in " + form.getKey());
                    }
                }
                for (String key : translated.keySet()) if (!english.containsKey(key)) fail(folder, key, "orphan translation");
            }
        }
        if (!languages.containsAll(Set.of("es", "pt", "fr"))) throw new IllegalStateException("Missing required catalogs: " + languages);
        System.out.println("Verified " + english.size() + " messages: English + " + languages);
    }

    private static void fail(Path folder, String key, String message) {
        throw new IllegalStateException(folder + ": " + key + ": " + message);
    }

    private static void selfTest() throws Exception {
        Path root = Files.createTempDirectory("floatingclock-translation-test");
        String valid = "<resources><string name=\"message\">Value %1$d%%</string></resources>";
        try {
            for (String folder : List.of("values", "values-es", "values-pt", "values-fr")) {
                Files.createDirectories(root.resolve(folder));
                Files.writeString(root.resolve(folder).resolve("strings.xml"), valid);
            }
            verify(root);
            var malformed = List.of(
                "<resources/>",
                valid.replace("%1$d", "%1$s"),
                valid.replace("Value %1$d%%", ""),
                valid.replace("</resources>", "<string name=\"message\">Duplicate</string></resources>"),
                valid.replace("</resources>", "<string name=\"orphan\">Extra</string></resources>"),
                "<resources><plurals name=\"message\"><item quantity=\"other\">%1$d</item></plurals></resources>",
                valid.replace("name=\"message\"", "name=\"message\" translatable=\"false\""));
            for (String fixture : malformed) {
                Files.writeString(root.resolve("values-fr/strings.xml"), fixture);
                boolean rejected = false;
                try { verify(root); } catch (IllegalStateException expected) { rejected = true; }
                if (!rejected) throw new AssertionError("Accepted malformed fixture: " + fixture);
            }
            System.out.println("Translation verifier: valid catalog accepted; 7 malformed fixtures rejected.");
        } finally {
            try (var paths = Files.walk(root)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }
}
