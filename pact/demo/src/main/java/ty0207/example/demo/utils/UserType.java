package ty0207.example.demo.utils;

import org.springframework.lang.Nullable;

public enum UserType {

    CREDITOR(1),
    DEBTOR(2);

    UserType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    @Nullable
    public static UserType resolve(int s) {
        for (UserType type : values()) {
            if (type.type == s) {
                return type;
            }
        }
        return null;
    }
    private final int type;

}
