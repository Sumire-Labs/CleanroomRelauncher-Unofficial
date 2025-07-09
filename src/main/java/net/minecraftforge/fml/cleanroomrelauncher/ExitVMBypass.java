package net.minecraftforge.fml.cleanroomrelauncher;

public class ExitVMBypass {

    public static void exit(int status) {
        exit$(status);
    }

    private static void exit$(int status) {
        exit$$(status);
    }

    private static void exit$$(int status) {
        System.exit(status);
    }

}
