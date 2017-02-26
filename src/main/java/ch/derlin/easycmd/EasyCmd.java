package ch.derlin.easycmd;

import ch.derlin.easycmd.accounts.Account;
import ch.derlin.easycmd.accounts.AccountsMap;
import ch.derlin.easycmd.console.Console;
import ch.derlin.easycmd.doc.CmdDoc;
import jline.console.completer.*;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.List;

/**
 * date: 15.02.17
 *
 * @author Lin
 */
public class EasyCmd {

    // algo for the deserialisation of data
    private static final String CRYPTO_ALGORITHM = "aes-128-cbc";
    private AccountsMap accounts;
    private List<String> results;
    private Console console;

    private String filepath;
    private String pass;

    @FunctionalInterface
    interface Commander {
        void apply(String command, String[] args);
    }

    private Map<String, Commander> commandMap;
    private CmdDoc doc;

    public static void main(String[] args) throws Exception {
        new EasyCmd(args);
        //Console c = new Console();c.test();

    }//end main

    public EasyCmd(String[] args) throws Exception {


        if (args.length < 1) {
            System.out.println("missing a filepath argument.");
            System.exit(1);
        }

        Options options = new Options();

        options.addOption("f", "file", true, "the session file");
        options.addOption("p", "pass", true, "the password (unsafe: added to history)");
        options.addOption("nocolor", "turn off the coloring in prompts");
        options.addOption("e", "encrypt", true, "encrypt the file given by -f and stop.");
        options.addOption("d", "decrypt", true, "decrypt the file given by -f and stop.");

        // parse the command line arguments
        CommandLine line = new DefaultParser().parse(options, args);

        // validate that block-size has been set
        if (!line.hasOption("file")) {
            // print the value of block-size
            System.out.println("missing file argument (-f <file>)");
            System.exit(0);
        }


        console = new Console(line.hasOption("nocolor"));
        filepath = line.getOptionValue("file");
        pass = line.getOptionValue("pass", "");

        boolean fileExists = new File(filepath).exists();
        if (fileExists) {
            if (line.hasOption("encrypt")) {
                // encrypt and quit
                String encryptPath = line.getOptionValue("encrypt");
                loadFromFile("encrypt", encryptPath);
                System.exit(accounts == null ? 1 : 0);
            } else {
                // decrypt file
                try {
                    while (pass.isEmpty()) pass = console.readPassword("password> ", "");
                    accounts = AccountsMap.fromEncryptedFile(filepath, pass);
                } catch (SerialisationManager.WrongCredentialsException e) {
                    System.out.println("Error: wrong credentials");
                    System.exit(0);
                }
            }

        } else {
            // ensure the user wants to create a new file
            console.warn("the file '%s' does not exist: ", filepath);
            if (!console.confirm("continue ?")) {
                System.exit(0);
            }

            // get a new password (confirm to avoid typing errors,
            // since it is not recoverable)
            if (pass.isEmpty()) {
                console.println("Choose a password. Ensure it is a strong one and don't forget it, it is not recoverable.");
                pass = getNewPass();
            }
            // creat empty
            accounts = new AccountsMap();
        }

        // decrypt and quit
        if (line.hasOption("decrypt")) {
            dumpToFile("dump", line.getOptionValues("decrypt"));
            System.exit(0);
        }

        results = accounts.keys();

        commandMap = new TreeMap<>();
        commandMap.put("find", this::findAll);
        commandMap.put("show", this::show);
        commandMap.put("showpass", this::showPass);
        commandMap.put("copy", this::copy);
        commandMap.put("edit", this::edit);
        commandMap.put("new", this::newAccount);
        commandMap.put("add", this::newAccount);
        commandMap.put("delete", this::deleteAccount);

        commandMap.put("load", this::loadFromFile);
        commandMap.put("dump", this::dumpToFile);


        commandMap.put("help", this::helpOrMan);
        commandMap.put("man", this::helpOrMan);

        commandMap.put("exit", (c, a) -> System.exit(1));

        // shortcuts
        commandMap.put("pass", (c, s) -> {
            ArrayList<String> list = new ArrayList<>(Arrays.asList(s));
            list.add(0, c);
            copy("copy", (String[]) list.toArray(new String[0]));
        });


        List<Completer> completors = new LinkedList<>();
        StringsCompleter fieldsCompleter = new StringsCompleter("name", "pseudo", "notes", "email");
        completors.add(
                new AggregateCompleter(
                        new ArgumentCompleter(new StringsCompleter("find"), fieldsCompleter, new NullCompleter()),
                        new ArgumentCompleter(new StringsCompleter("show"), new NullCompleter()),
                        new ArgumentCompleter(new StringsCompleter("showpass"), new NullCompleter()),
                        new ArgumentCompleter(new StringsCompleter("copy"), fieldsCompleter),
                        new ArgumentCompleter(new StringsCompleter("edit"), new NullCompleter()),
                        new ArgumentCompleter(new StringsCompleter("new"), new NullCompleter()),
                        new ArgumentCompleter(new StringsCompleter("add"), new NullCompleter()),
                        new ArgumentCompleter(new StringsCompleter("load"), new FileNameCompleter()),
                        new ArgumentCompleter(new StringsCompleter("dump"), new FileNameCompleter()),
                        new ArgumentCompleter(new StringsCompleter("exit"), new NullCompleter()),
                        new ArgumentCompleter(new StringsCompleter("pass"), new NullCompleter()),
                        new ArgumentCompleter(new StringsCompleter("man"), new NullCompleter()),
                        new ArgumentCompleter(new StringsCompleter("help"),
                                new StringsCompleter(commandMap.keySet().toArray(new String[0])), new NullCompleter())
                )
        );
        for (Completer c : completors) {
            console.addCompleter(c);
        }

        InputStream stream = getClass().getResourceAsStream("/man.json");
        doc = new CmdDoc(stream);

        interpreter();

    }//end main


    public void interpreter() throws IOException {
        while (true) {
            // history enabled only for commands
            console.setHistoryEnabled(true);
            String line = console.readLine();
            console.setHistoryEnabled(false);
            if (!line.isEmpty()) {
                doCommand(line.split(" +"));
                System.out.println();
            }
        }

    }

    public void doCommand(String[] split) {
        String cmd = split[0].toLowerCase();
        String[] args = Arrays.copyOfRange(split, 1, split.length);
        if (commandMap.containsKey(cmd)) {
            commandMap.get(cmd).apply(cmd, args);
        } else {
            CmdDoc.CmdDescription bestMatch = doc.betterMatch(cmd, 2);
            if (bestMatch != null) {
                String bestCmd = bestMatch.getName();
                System.out.printf("unrecognized command. Assuming '%s'%n", bestCmd);
                commandMap.get(bestCmd).apply(bestCmd, args);
            } else {
                console.info("unrecognized command. Assuming find.");
                commandMap.get("find").apply("find", split);
            }
        }

    }

    public void findAll(String cmd, String... args) {
        List<String> newResults;
        if (args.length < 1) {
            newResults = accounts.keys();
        } else {
            newResults = accounts.find(args);
        }
        if (newResults.size() > 0) {
            results = newResults;
            printResults();
        } else {
            console.warn("not match.");
        }
    }

    public void show(String cmd, String... args) {
        Account a = findOne(args);
        if (a != null) a.show(console);
    }

    public void copy(String cmd, String... args) {
        if (args.length < 1) {
            console.error("incomplete command.");
            return;
        }

        Account a = findOne(Arrays.copyOfRange(args, 1, args.length));
        if (a != null) {
            String fieldname = args[0];
            String field = a.get(fieldname);
            if (field == null) {
                console.error("invalid field " + args[0]);
            } else if (field.isEmpty()) {
                console.warn("nothing to copy (empty field)");
            } else {
                copy(field);
                console.info("%s for account '%s' copied to clipboard%n", fieldname, a.name);
            }
        }
    }

    public void edit(String cmd, String... args) {
        Account old = findOne(args);
        if (old == null) return;

        try {
            Account nw = console.readAccount(old);
            if (old.equals(nw)) {
                // no changes
                console.println("   nothing to save.");
                return;
            }

            if (!old.name.equals(nw.name) && accounts.containsKeyLower(nw.name)) {
                if (!console.confirm("   another account with this name already exists. override ?")) return;

            } else {
                // same name, ask for confirmation
                if (!console.confirm("   save changes ?")) return;
            }

            accounts.remove(old.name);
            old.overrideWith(nw);
            accounts.put(old.name, old);
            save();

        } catch (IOException e) {
            console.error("error editing account.");
        }
    }


    public void newAccount(String cmd, String... args) {

        try {
            Account empty = new Account();
            empty.name = String.join(" ", args);
            Account a = console.readAccount(empty);
            if (a.name.isEmpty()) {
                console.error("empty name");
                return;
            }

            if (accounts.containsKeyLower(a.name)) {
                if (!console.confirm("   another account with this name already exists. override ?")) return;
            } else {
                if (!console.confirm("   Save changes?")) return;
            }

            accounts.put(a.name, a);
            save();

        } catch (IOException e) {
            console.error("error saving account.");
        }
    }

    public void deleteAccount(String cmd, String[] args) {
        Account a = findOne(args);
        if (a == null) return;

        try {
            if (console.confirm(String.format(" delete account '%s' ?", a.name))) {
                accounts.remove(a.name);
                save();
            }
        } catch (IOException e) {
            console.error(e.getMessage());
        }
    }

    public void showPass(String cmd, String[] args) {
        Account a = findOne(args);
        if (a == null) return;

        if (a.password.isEmpty()) {
            console.warn("empty password.");
        } else {
            console.showPassword(a.password);
            System.out.println();
        }
    }

    public void loadFromFile(String cmd, String... arg) {
        if (arg.length == 0) {
            console.error("missing file. Usage: %s <filepath:string>", cmd);
            return;
        }
        String to = arg[0];
        if (!canWriteTo(to)) return;
        try {
            accounts = AccountsMap.fromFile(filepath);
            filepath = to;
            if (pass.isEmpty()) pass = getNewPass();
            save();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.printf("Error loading json file '%s'%n", filepath);
        }
    }

    public void dumpToFile(String cmd, String[] arg) {
        try {
            if (arg.length == 0) {
                console.error("missing file. Usage: %s <filepath:string> [noIndent:boolean]", cmd);
                return;
            }

            if (accounts.size() == 0) {
                console.warn(" Nothing to dump; empty accounts list.");
                return;
            }

            boolean indent = true;
            if (arg.length > 1) {
                indent = !Boolean.parseBoolean(arg[1]);
            }

            String to = arg[0];
            if (!canWriteTo(to)) return;

            AccountsMap.toFile(to, accounts, indent);
            console.info("saved to '%s'", to);

        } catch (IOException e) {
            console.error(e.getMessage());
        }
    }

    private void save() {
        try {
            accounts.save(filepath, pass);
            console.info("saved.");
        } catch (IOException e) {
            console.error("error saving file.");
        }
    }

    public void helpOrMan(String cmd, String[] args) {
        // no arguments, print the list of available commands
        if (args.length == 0) {
            System.out.println(cmd.equals("man") ? doc.man() : doc.help());
        } else {
            // if a command name was specified, print its description
            String param = args[0];
            CmdDoc.CmdDescription descr = doc.get(param);

            if (descr != null) {
                System.out.println(descr.fullDescription());
            } else {
                // the command does not exist -> print the closest command name available
                System.out.println(doc.help());
            }
        }

    }//end help

    /* *****************************************************************
     * private utils
     * ****************************************************************/

    private boolean canWriteTo(String filepath) {
        File f = new File(filepath);
        if (f.exists()) {
            // if not a file, abort
            if (!f.isFile()) {
                console.error("'%s' is not a regular file.", filepath);
                return false;
            }

            // else, ask for overwrite confirmation
            try {
                return console.confirm(String.format("'%s' already exists, overwrite ?", f.getAbsolutePath()));
            } catch (IOException e) {
                return false;
            }
        }
        return true;
    }

    private String getNewPass() throws IOException {
        String pass1, pass2;
        while (true) {
            // @formatter:off
                    do{ pass1 = console.readPassword("password> ", ""); } while (pass1.isEmpty());
                    do{ pass2 = console.readPassword("confirm> ", ""); } while (pass2.isEmpty()) ;
                    // @formatter:on
            if (pass1.equals(pass2)) break;
            console.error("passwords do  not match%n");
        }
        return pass1;
    }

    private void printResults() {
        int i = 0;
        for (String name : results) {
            System.out.printf("  [%d] %s%n", i++, name);
        }//end for
        System.out.printf(" %d results.%n", i);
    }

    private void copy(String s) {
        StringSelection selection = new StringSelection(s);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);
    }

    private Account findOne(String... args) {

        // no argument: ok only if results has only one account
        if (args.length == 0) {
            if (results.size() > 1) {
                console.error("missing index");
                return null;
            } else {
                return accounts.isEmpty() ? null : accounts.get(results.get(0));
            }
        }

        // check for integer/index argument
        try {
            int i = Integer.parseInt(args[0]);
            if (i >= 0 && i < results.size()) {
                return accounts.get(results.get(i));
            } else {
                console.error("argument not in range 0:" + results.size());
                return null;
            }
        } catch (NumberFormatException e) {
        }

        // finally, check if the arguments match only one account
        List<String> res = accounts.find(args);
        if (res.size() == 1) {
            results = res;
            return accounts.isEmpty() ? null : accounts.get(results.get(0));
        } else {
            console.error("ambiguous account.");
            return null;
        }
    }
}
