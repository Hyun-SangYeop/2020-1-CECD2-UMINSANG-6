public class Identifier {
    String name;
    String pttn;
    Validator valid;

    public Identifier(String name, String pttn, Validator valid) {
        this.name = name;
        this.pttn = pttn;
        this.valid = valid;
    }

    @Override
    public String toString() {
        return "Idf(" + name + ")";
    }

    interface Validator {
        boolean check(String... groups);
    }
}
