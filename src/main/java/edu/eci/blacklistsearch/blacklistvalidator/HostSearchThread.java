package edu.eci.blacklistsearch.blacklistvalidator;

import edu.eci.blacklistsearch.spamkeywordsdatasource.HostBlacklistsDataSourceFacade;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class HostSearchThread extends Thread {

    private final String ipaddress;
    private final int startIndex;
    private final int endIndex;

    private final AtomicInteger ocurrenciasTotales;
    private final int alarmCount;

    private final List<Integer> blackListOcurrences = new LinkedList<>();

    private int checkedListsCount = 0;

    public HostSearchThread(String ipaddress, int startIndex, int endIndex, AtomicInteger ocurrenciasTotales, int alarmCount) {
        this.ipaddress = ipaddress;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.ocurrenciasTotales = ocurrenciasTotales;
        this.alarmCount = alarmCount;
    }

    @Override
    public void run() {

        HostBlacklistsDataSourceFacade skds = HostBlacklistsDataSourceFacade.getInstance();

        for (int i = startIndex; i < endIndex && ocurrenciasTotales.get() < alarmCount; i++) {
            checkedListsCount++;
            if (skds.isInBlackListServer(i, ipaddress)) {
                blackListOcurrences.add(i);
                ocurrenciasTotales.incrementAndGet();
            }
        }

    }

    public int getOcurrencesCount() {
        return blackListOcurrences.size();
    }

    public List<Integer> getBlackListOcurrences() {
        return blackListOcurrences;
    }

    public int getCheckedListsCount() {
        return checkedListsCount;
    }

}
