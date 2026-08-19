/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.eci.blacklistsearch.blacklistvalidator;

import java.util.List;

/**
 *
 * @author hcadavid
 */
public class Main {

    private static final String IP="202.24.34.55";

    public static void main(String a[]){

        int cores=Runtime.getRuntime().availableProcessors();
        System.out.println("Nucleos de procesamiento disponibles: "+cores);

        //con 100 hilos la busqueda dura 1 segundo muy poco para que jVisualVM
        //for (int i=0;i<5;i++){
        // buscar(IP,100);
        //}

        buscar(IP,500);
        //buscar(IP,cores*2);
    }

    private static void buscar(String ipaddress,int n){

        HostBlackListsValidator hblv=new HostBlackListsValidator();

        long inicio=System.currentTimeMillis();
        List<Integer> blackListOcurrences=hblv.checkHost(ipaddress, n);
        long tiempo=System.currentTimeMillis()-inicio;

        System.out.println("Hilos: "+n+" - Tiempo: "+tiempo+" ms");
        System.out.println("The host was found in the following blacklists:"+blackListOcurrences);

    }

}
