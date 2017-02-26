package ch.derlin.easycmd.accounts;

import ch.derlin.easycmd.console.Console;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * User: lucy
 * Date: 14/09/13
 * Version: 0.1
 */
public class Account {
    public String name = "", pseudo = "", email = "", password = "", notes = "";

    @SerializedName("creation date")
    public String creationDate;
    @SerializedName("modification date")
    public String modificationDate;

    @FunctionalInterface
    public interface EditAccountChecker {
        boolean apply(Account old, Account nw);
    }

    public boolean matches(Pattern pattern) {
        return pattern.matcher(name).matches() ||
                pattern.matcher(pseudo).matches() ||
                pattern.matcher(email).matches() ||
                pattern.matcher(notes).matches();
    }

    public boolean contains(String... patterns) {
        for (String pattern : patterns) {
            pattern = pattern.toLowerCase();
            boolean match = name.toLowerCase().contains(pattern) ||
                    pseudo.toLowerCase().contains(pattern) ||
                    email.toLowerCase().contains(pattern) ||
                    notes.toLowerCase().contains(pattern);
            if (!match) return false;
        }//end for
        return true;
    }

    public String get(String field) {
        field = field.toLowerCase();
        if (field.equals("name")) return name;
        if (field.equals("pseudo")) return pseudo;
        if (field.equals("email")) return email;
        if (field.startsWith("pass")) return password;
        if (field.startsWith("note")) return notes;
        return null;
    }


    public void show(Console console) {
        console.printWithPrompt("   name: ", name);
        console.printWithPrompt("   pseudo: ", pseudo);
        if (!email.isEmpty()) console.printWithPrompt("    email: ", email);
        console.printWithPrompt("   notes: ", notes);
    }

    public boolean edit(Console console, EditAccountChecker checker) throws IOException {
        Account newAccount = new Account();
        System.out.println();

        newAccount.name = console.readWithDefault("   name> ", name).trim();
        newAccount.pseudo = console.readWithDefault("   pseudo> ", pseudo).trim();
        if (!email.isEmpty()) newAccount.email = console.readWithDefault("   email> ", email).trim();
        newAccount.password = console.readPassword("   password> ", password).trim();
        newAccount.notes = console.readWithDefault("   note> ", notes).trim();

        console.println();

        if (checker.apply(this, newAccount)) this.overrideWith(newAccount);
        console.println();
        return true;
    }

    public void overrideWith(Account other) {
        this.name = other.name;
        this.pseudo = other.pseudo;
        this.email = other.email;
        this.password = other.password;
        this.notes = other.notes;
        String now = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
        if (creationDate == null || creationDate.isEmpty()) creationDate = now;
        this.modificationDate = now;
    }

    @Override
    public boolean equals(Object obj) {
        return this.hashCode() == obj.hashCode();
    }

    @Override
    public int hashCode() {
        return name.hashCode() + pseudo.hashCode() + password.hashCode() + notes.hashCode();
    }

    @Override
    public String toString() {
        return String.format("{name=%s, pseudo=%s, pass=%s, created=%s, modified=%s}",
                name, pseudo, password, creationDate, modificationDate);
    }



}//end class
