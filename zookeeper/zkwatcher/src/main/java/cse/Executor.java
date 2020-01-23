package cse;
/**
 * A simple example program to use DataMonitor to start and
 * stop executables based on a znode. The program watches the
 * specified znode and saves the data that corresponds to the
 * znode in the filesystem. It also starts the specified program
 * with the specified arguments when the znode exists and kills
 * the program if the znode goes away.
 */

import java.io.BufferedReader;

import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.HashMap;
import java.util.Map;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

public class Executor
    implements Watcher, Runnable, DataMonitor.DataMonitorListener
{
  private String znode;

  private DataMonitor dm;

  private ZooKeeper zk;

  private String filename;

  private Process child;

  private Executor(String hostPort, String znode, String filename) throws IOException {
    this.filename = filename;
    this.znode = znode;
    // get name in system env
    Map<String, String> map = System.getenv();
    String selfDNS = map.get("DNS");
    FileReader fr = new FileReader(filename);
    BufferedReader bf = new BufferedReader(fr);
    String str;
    String last = "";
    // the last line can get ip in the source
    while ((str = bf.readLine()) != null) {
      last = str;
    }
    bf.close();
    fr.close();
    String selfIP = last.split(" ")[0];
    System.out.println("check DNS ENV");
    System.out.println(selfDNS);
    System.out.println(selfIP);
    // get ip in original /etc/hosts
    zk = new ZooKeeper(hostPort, 3000, this);
    // register data may failed
    Map<String ,String> entries = new HashMap<>();
    // load map
    boolean hasRegister = false;
    while(!hasRegister) {
      try{
        Stat stat = zk.exists(znode, true);
        int prevVersion = stat.getVersion();
        byte[] data = zk.getData(znode, true, stat);
        String[] dns = new String(data).split("\n");

        for (String d : dns) {
          String[] entry = d.split(" ");
          if (entry.length != 2) {
            System.out.println("host & ip are not parsed!");
            System.exit(1);
          }
          entries.put(entry[1], entry[0]);
        }
        // insert or rewrite entry
//        System.out.println("entries size ");
////        System.out.println(entries.size());
        entries.put(selfDNS, selfIP);

        StringBuilder dumps = new StringBuilder();
        // dump map
        for(Map.Entry<String, String> mm : entries.entrySet()) {
          dumps.append(mm.getValue()).append(" ").append(mm.getKey()).append("\n");
        }


        System.out.println(dumps.toString());
        zk.setData(znode, dumps.toString().getBytes(), prevVersion);
        hasRegister = true;
      } catch (KeeperException | InterruptedException e) {
        e.printStackTrace();
      }
    }
    dm = new DataMonitor(zk, znode, null, this);
  }

  /**
   * @param args init parameter for process
   */
  public static void main(String[] args) {
    if (args.length < 3) {
      System.err
          .println("USAGE: Executor hostPort znode filename program [args ...]");
      System.exit(2);
    }
    String hostPort = args[0];
    String znode = args[1];
    String filename = args[2];
    try {
      new Executor(hostPort, znode, filename).run();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /***************************************************************************
   * We do process any events ourselves, we just need to forward them on.
   *
   * @see org.apache.zookeeper.Watcher#process(org.apache.zookeeper.proto.WatcherEvent)
   */
  public void process(WatchedEvent event) {
    dm.process(event);
  }

  public void run() {
    try {
      synchronized (this) {
        while (!dm.dead) {
          wait();
        }
      }
    } catch (InterruptedException e) {
    }
  }

  public void closing(int rc) {
    synchronized (this) {
      notifyAll();
    }
  }

  static class StreamWriter extends Thread {
    OutputStream os;

    InputStream is;

    StreamWriter(InputStream is, OutputStream os) {
      this.is = is;
      this.os = os;
      start();
    }

    public void run() {
      byte b[] = new byte[80];
      int rc;
      try {
        while ((rc = is.read(b)) > 0) {
          os.write(b, 0, rc);
        }
      } catch (IOException ignored) {
      }

    }
  }

  public void exists(byte[] data) {
    if (data == null) {
      if (child != null) {
        System.out.println("Killing process");
        child.destroy();
        try {
          child.waitFor();
        } catch (InterruptedException ignored) {
        }
      }
      child = null;
    } else {
      if (child != null) {
        System.out.println("Stopping child");
        child.destroy();
        try {
          child.waitFor();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      try {
        FileOutputStream fos = new FileOutputStream(filename);

        fos.write(data);
        fos.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
//      try {
//        System.out.println("Starting child");
//        child = Runtime.getRuntime().exec(exec);
//        new StreamWriter(child.getInputStream(), System.out);
//        new StreamWriter(child.getErrorStream(), System.err);
//      } catch (IOException e) {
//        e.printStackTrace();
//      }
    }
  }
}