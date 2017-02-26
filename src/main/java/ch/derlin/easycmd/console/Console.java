package ch.derlin.easycmd.console;

import ch.derlin.easycmd.accounts.Account;
import jline.console.ConsoleReader;

import java.io.IOException;

import static ch.derlin.easycmd.console.Console.ANSIColors.*;


/**
 * date: 15.02.17
 *
 * @author Lin
 */
public class Console extends ConsoleReader {

    // use: readLine("password>", PASSWORD_MASK);
    public static final Character PASSWORD_MASK = new Character('*');
    private static final String PASSWORD_REPLACE = "********************************************************************";

    private String prompt, promptColor;
    private boolean disableColor;

    public Console() throws Exception {
        this("> ", ANSIColors.YELLOW, false);
    }

    public Console(boolean noColor) throws Exception {
        this("> ", ANSIColors.YELLOW, noColor);
    }

    public Console(String prompt, String promptColor, boolean noColor) throws Exception {
        super();
        this.prompt = prompt;
        this.promptColor = promptColor;
        this.disableColor = noColor;
        super.setPrompt(wrap(prompt, promptColor));
    }

    @Override
    public String readLine() throws IOException {
        return super.readLine(wrap(prompt, promptColor));
    }

    public String readPassword(String prompt, String preload) throws IOException {
        String ret = readLine(wrap(prompt, promptColor), PASSWORD_MASK);
        return ret.isEmpty() ? preload : ret;
    }

    public String readWithDefault(String prompt, String preload) throws IOException {
        //resetLine();
        //resetPromptLine(prompt, preload, preload.length());
        return readLine(wrap(prompt, promptColor), null, preload);
    }

    public boolean confirm(String text) throws IOException {
        return readLine(wrap(text + " [y|N] " + prompt, promptColor)).trim().equals("y");
    }

    public boolean clearScreen() throws IOException {
        boolean ok = super.clearScreen();
        flush();
        return ok;
    }

    public void printWithPrompt(String prompt, String text) {
        System.out.printf("%s%s%n", wrap(prompt, promptColor), text);
    }

    public void warn(String warn, Object... args) {
        String text = String.format(warn, args);
        System.out.println(wrap(" warn: ", PURPLE) + text);
    }

    public void error(String error, Object... args) {
        String text = String.format(error, args);
        System.out.println(wrap(" error: ", RED) + text);
    }

    public void info(String info, Object... args) {
        String text = String.format(info, args);
        System.out.println(text);
    }


    public String wrap(String text, String color) {
        return disableColor ? text : String.format("%s%s%s", color, text, RESET);
    }


    public Account readAccount(Account oldAccount) throws IOException {
        if (oldAccount == null) oldAccount = new Account();
        Account newAccount = new Account();
        System.out.println();

        newAccount.name = readWithDefault("   name> ", oldAccount.name).trim();
        newAccount.pseudo = readWithDefault("   pseudo> ", oldAccount.pseudo).trim();
        if (!oldAccount.email.isEmpty()) newAccount.email = readWithDefault("   email> ", oldAccount.email).trim();
        newAccount.password = readPassword("   password> ", oldAccount.password).trim();
        newAccount.notes = readWithDefault("   note> ", oldAccount.notes).trim();

        return newAccount;
    }

    public boolean showPassword(String pass) {
        try {
            resetPromptLine("   password> ", pass, pass.length());
            readCharacter();
            if (setCursorPosition(0) && killLine()) resetPromptLine("   password>", "", 0);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static class ANSIColors {
        public static final String RED = "\033[00;31m";
        public static final String GREEN = "\033[00;32m";
        public static final String YELLOW = "\033[00;33m";
        public static final String BLUE = "\033[00;34m";
        public static final String PURPLE = "\033[00;35m";
        public static final String CYAN = "\033[00;36m";
        public static final String LIGHTGRAY = "\033[00;37m";
        public static final String LRED = "\033[01;31m";
        public static final String LGREEN = "\033[01;32m";
        public static final String LYELLOW = "\033[01;33m";
        public static final String LBLUE = "\033[01;34m";
        public static final String LPURPLE = "\033[01;35m";
        public static final String LCYAN = "\033[01;36m";
        public static final String WHITE = "\033[01;37m";
        public static final String RESET = "\033[39;49m";

    }
}
