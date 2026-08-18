/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.eci.blacklistsearch.blacklistvalidator;

import edu.eci.blacklistsearch.spamkeywordsdatasource.HostBlacklistsDataSourceFacade;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author hcadavid
 */
public class HostBlackListsValidator {

    private static final int BLACK_LIST_ALARM_COUNT=5;

    /**
     * Check the given host's IP address in all the available black lists,
     * and report it as NOT Trustworthy when such IP was reported in at least
     * BLACK_LIST_ALARM_COUNT lists, or as Trustworthy in any other case.
     * The search space is split among n threads, and the search stops as soon
     * as the threads have jointly found BLACK_LIST_ALARM_COUNT occurrences:
     * the shared counter is an AtomicInteger, so the threads can increment and
     * read it concurrently without race conditions.
     * @param ipaddress suspicious host's IP address.
     * @param n number of threads the search is distributed among.
     * @return  Blacklists numbers where the given host's IP address was found.
     */
    public List<Integer> checkHost(String ipaddress,int n){
        
        LinkedList<Integer> blackListOcurrences=new LinkedList<>();
        AtomicInteger ocurrencias = new AtomicInteger(0);
        int listasRevisadas = 0;
        HostBlacklistsDataSourceFacade skds = HostBlacklistsDataSourceFacade.getInstance();
        
        
        LinkedList<HostSearchThread> hilos = new LinkedList<>();

        int totalListas = skds.getRegisteredServersCount();
        int tamSegmento = totalListas / n;

        for (int i=0;i<n;i++){
            int inicio = i * tamSegmento;
            int fin = (i == n-1) ? totalListas : inicio + tamSegmento;

            HostSearchThread host = new HostSearchThread(ipaddress, inicio, fin, ocurrencias, BLACK_LIST_ALARM_COUNT);
            hilos.add(host);
            host.start();
        }

        for (HostSearchThread host : hilos){
            try {
                host.join();
            } catch (InterruptedException ex) {
                LOG.log(Level.SEVERE, "Busqueda interrumpida", ex);
                Thread.currentThread().interrupt();
            }
            blackListOcurrences.addAll(host.getBlackListOcurrences());
            listasRevisadas  +=  host.getCheckedListsCount();
        }
        
        if (ocurrencias.get() >= BLACK_LIST_ALARM_COUNT){
            skds.reportAsNotTrustworthy(ipaddress);
        }
        else{
            skds.reportAsTrustworthy(ipaddress);
        }                
        
        LOG.log(Level.INFO, "Checked Black Lists:{0} of {1}", new Object[]{listasRevisadas , skds.getRegisteredServersCount()});
        
        return blackListOcurrences;
    }
    
    
    private static final Logger LOG = Logger.getLogger(HostBlackListsValidator.class.getName());

}
