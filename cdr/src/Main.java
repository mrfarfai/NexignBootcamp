import java.io.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


public class Main {
    public static void main(String[] args) {
        File FILE_PATH = new File("cdr.txt");
        HashMap<String, Client> logs = new HashMap<>();
        try (BufferedReader inputStreamReader = new BufferedReader(new InputStreamReader(new FileInputStream(FILE_PATH)))) {
            String nextLine;
            String[] data;
            while ((nextLine = inputStreamReader.readLine()) != null) {
                data = nextLine.split(", ");
                String key = data[1] + "_" + data[4];
                if(!logs.containsKey(key)) {
                    logs.put(key, new Client(data));
                }
                else {
                    Client client = logs.get(key);
                    client.tarif.call(data);
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }

        for (Map.Entry<String, Client> client : logs.entrySet()) {
            String key = client.getKey();
            File SAVE_PATH = new File("reports/report_" + key + ".txt");
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream((SAVE_PATH))))) {
                String data = "";
                data += "Tariff index: " + key.split("_")[1] + "\n";
                data += "----------------------------------------------------------------------------\n";
                data += "Report for phone number " + key.split("_")[0] + ":\n";
                data += "----------------------------------------------------------------------------\n";
                data += "| Call Type |   Start Time        |     End Time        | Duration | Cost  |\n";
                data += "----------------------------------------------------------------------------\n";

                Tarif tarif = client.getValue().tarif;

                ArrayList<History> histories = new ArrayList<History>(client.getValue().tarif.histories);
                Collections.sort(histories, new Sort());
                for (History history : histories) {
                    long h = history.getDuration() / 3600;
                    long m = history.getDuration() % 3600 / 60;
                    long s = (history.getDuration() % 3600) % 60;
                    data += "|     " + history.getCallType() +
                            "    | " + history.getTalkStart().split("T")[0] + " " + history.getTalkStart().split("T")[1] +
                            " | " + history.getTalkEnd().split("T")[0] + " " + history.getTalkEnd().split("T")[1] +
                            " | " + h / 10 + h % 10 + ":" +  m / 10 + m % 10 + ":" +  s / 10 + s % 10 +
                            " |  " + history.getCost() + " |\n";
                }

                data += "----------------------------------------------------------------------------\n";
                data += "|                                           Total Cost: |     " + tarif.getTotalCost() + " rubles |\n";
                data += "----------------------------------------------------------------------------\n";
                writer.write(data);
            } catch (Exception e){
                System.out.println(e);
            }
        }
    }
}

class Sort implements Comparator<History> {
    public int compare(History h1, History h2) {
        return h1.getTalkStart().compareTo(h2.getTalkStart());
    }
}

class Client {
    Tarif tarif;
    public Client (String[] data){
        tarif = new Tarif(data);
    }
}

class Tarif {

    ArrayList<History> histories = new ArrayList<>();

    private double cost;
    private double totalCost;
    private int totalMinute = 0;
    private String tarifType;

    public Tarif (String[] data) {
        this.tarifType = data[4];
        if(tarifType.equals("03")) {
            this.cost = 1.5;
        }
        else if(tarifType.equals("06")) {
            this.cost = 1;
            this.totalCost = 100;
        }
        else {
            this.cost = 1.5;
        }
        call(data);
    }

    public void call(String[] data){
        double currentCost = 0;
        History history = new History(data);
        if(data[0].equals("01")) {
            int durationMinute = (int) Math.ceil(history.getDuration() / 60.0);
            if(this.tarifType.equals("03")) {
                currentCost += durationMinute * this.cost;
                totalCost += currentCost;
            }
            else if(this.tarifType.equals("06")) {
                if(totalMinute < 300) {
                    int mn = Math.min(300 - totalMinute, durationMinute);
                    totalMinute += mn;
                    durationMinute -= mn;
                }
                currentCost = durationMinute * this.cost;
                totalCost += currentCost;
            }
            else if(this.tarifType.equals("11")) {
                if(totalMinute < 100) {
                    int mn = Math.min(100 - totalMinute, durationMinute);
                    totalMinute += mn;
                    durationMinute -= mn;
                    currentCost = mn * 0.5;
                }
                currentCost += durationMinute * this.cost;
                totalCost += currentCost;
            }
        }

        history.addPrice(currentCost);
        histories.add(history);
    }

    public double getTotalCost() {
        return totalCost;
    }

}

class History {
    private LocalDateTime talkStart;
    private LocalDateTime talkEnd;
    private Duration duration;
    private double cost;
    private String callType;

    public History(String[] data) {
        callType = data[0];
        talkStart = LocalDateTime.parse(
                data[2].substring(0, 4) + "-" +
                        data[2].substring(4, 6) + "-" +
                        data[2].substring(6, 8) + " " +
                        data[2].substring(8, 10) + ":" +
                        data[2].substring(10, 12) + ":" +
                        data[2].substring(12),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        talkEnd = LocalDateTime.parse(
                data[3].substring(0, 4) + "-" +
                        data[3].substring(4, 6) + "-" +
                        data[3].substring(6, 8) + " " +
                        data[3].substring(8, 10) + ":" +
                        data[3].substring(10, 12) + ":" +
                        data[3].substring(12),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        duration = Duration.between(talkStart, talkEnd);
    }

    public void addPrice(double cost){
        this.cost = cost;
    }

    public String getTalkEnd() {
        return String.valueOf(talkEnd);
    }

    public long getDuration() {
        return duration.getSeconds();
    }

    public String getTalkStart() {
        return String.valueOf(talkStart);
    }

    public double getCost() {
        return cost;
    }

    public String getCallType() {
        return callType;
    }
}