package com.skcraft.launcher.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.Lists;
import com.skcraft.launcher.persistence.Scrambled;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang.RandomStringUtils;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Persisted account list
 */
@Scrambled("ACCOUNT_LIST_NOT_SECURITY!")
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountList {
    private List<SavedSession> accounts = Lists.newArrayList();
    private String clientId = RandomStringUtils.randomAlphanumeric(24);

    @JsonIgnore private final transient List<Consumer<AccountList>> listeners = new CopyOnWriteArrayList<>();

	public synchronized void add(SavedSession session) {
		accounts.add(session);
        notifyListeners();
	}

	public synchronized void remove(SavedSession session) {
        if (accounts.remove(session)) {
            notifyListeners();
		}
	}

	public synchronized void update(SavedSession newSavedSession) {
		int index = accounts.indexOf(newSavedSession);

		if (index > -1) {
			accounts.set(index, newSavedSession);
		} else {
			this.add(newSavedSession);
            return;
		}
        notifyListeners();
	}

    @JsonIgnore
    public synchronized List<SavedSession> snapshot() {
        return Lists.newArrayList(accounts);
	}

    public void addListener(Consumer<AccountList> listener) {
        listeners.add(listener);
	}

    public void removeListener(Consumer<AccountList> listener) {
        listeners.remove(listener);
	}

    private void notifyListeners() {
        for (Consumer<AccountList> listener : listeners) {
            listener.accept(this);
        }
	}
}
