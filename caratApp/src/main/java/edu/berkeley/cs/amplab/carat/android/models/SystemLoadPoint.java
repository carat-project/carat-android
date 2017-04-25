package edu.berkeley.cs.amplab.carat.android.models;

/**
 * Created by Jonatan Hamberg on 2.2.2017.
 */
public class SystemLoadPoint {
    public float user, system, nice, idle, iowait, irq, softirq, steal, guest,
        guestnice, systemAll, idleAll, total, virtualAll, userAll, niceAll;

    public SystemLoadPoint(int[] data){
        this.user =         data[1];
        this.system =       data[2];
        this.nice =         data[3];
        this.idle =         data[4];
        this.iowait =       data[5];
        this.irq =          data[6];
        this.softirq =      data[7];
        this.steal =        data[8];
        this.guest =        data[9];
        this.guestnice =    data[10];

        this.userAll = user - guest;
        this.niceAll = nice - guestnice;
        this.idleAll = idle + iowait;
        this.systemAll = system + irq + softirq;
        this.virtualAll = guest + guestnice;
        this.total = userAll + niceAll + systemAll + idleAll + steal + virtualAll;
    }
}
