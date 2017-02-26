package ch.derlin.easycmd.accounts;

import ch.derlin.easycmd.SerialisationManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * date: 16.02.17
 *
 * @author Lin
 */
public class AccountsMap extends TreeMap<String, Account> {

    // algo for the deserialisation of data
    private static final String CRYPTO_ALGORITHM = "aes-128-cbc";

    // ------------------------------------- constructors
    public AccountsMap() {
        super();
    }

    public AccountsMap(List<Account> accounts) {
        super();
        accounts.stream().forEach(a -> {
            put(a.name, a);
        });
    }

    // ------------------------------------- search

    public List<String> keys() {
        return keySet().stream().collect(Collectors.toList());
    }

    public boolean containsKeyLower(String s) {
        final String sLower = s.toLowerCase().replace(" +", " ");
        for (String k : keys()) {
            if (k.toLowerCase().replace(" +", " ").equals(sLower)) return true;
        }//end for
        return false;
    }

    public List<String> find(String... patterns) {
        return values().stream()
                .filter(a -> a.contains(patterns))
                .map(a -> a.name)
                .collect(Collectors.toList());
    }

    public List<String> findR(String pattern) {
        Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        return values().stream()
                .filter(a -> a.matches(p))
                .map(a -> a.name)
                .collect(Collectors.toList());
    }

    public void save(String filepath, String pass) throws IOException {
        AccountsMap.toEncryptedFile(filepath, pass, this);
    }


    /* *****************************************************************
     * static utils
     * ****************************************************************/


    public static AccountsMap fromFile(String filepath) throws IOException, SerialisationManager.WrongCredentialsException {
        //@formatter:off
        List<Account> deserialized = new Gson().fromJson(new FileReader(filepath),
                new TypeToken<List<Account>>() {}.getType());
        //@formatter:on
        return new AccountsMap(deserialized);
    }

    public static void toFile(String filepath, AccountsMap accounts, boolean indent) throws IOException {
        try (OutputStream out = new FileOutputStream(new File(filepath))) {
            Gson gson = indent ? new GsonBuilder().setPrettyPrinting().create() : new GsonBuilder().create();
            out.write(gson.toJson(accounts.values()).getBytes());
        }
    }

    public static AccountsMap fromEncryptedFile(String filepath, String password) throws IOException, SerialisationManager.WrongCredentialsException {
        List<Account> deserialized = (List<Account>) SerialisationManager.deserialize(CRYPTO_ALGORITHM, filepath, password,
                new TypeToken<List<Account>>() {
                }.getType());
        return new AccountsMap(deserialized);
    }

    public static void toEncryptedFile(String filepath, String password, AccountsMap accounts) throws IOException {
        SerialisationManager.serialize(new ArrayList(accounts.values()), CRYPTO_ALGORITHM, filepath, password);
    }
}
