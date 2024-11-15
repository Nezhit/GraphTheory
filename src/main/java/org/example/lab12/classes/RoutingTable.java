package org.example.lab12.classes;

import java.util.HashMap;
import java.util.Map;

public class RoutingTable {
    private Map<Integer, Map<Integer, Integer>> routingData; // {destination: {viaNode: hopCount}}

    public RoutingTable() {
        this.routingData = new HashMap<>();
    }

    public void updateTable(int destination, int viaNode, int hopCount) {
        routingData.putIfAbsent(destination, new HashMap<>());
        Map<Integer, Integer> hops = routingData.get(destination);
        hops.put(viaNode, Math.min(hops.getOrDefault(viaNode, Integer.MAX_VALUE), hopCount));
    }

    public String getTableRepresentation() {
        StringBuilder sb = new StringBuilder();
        sb.append("Пункт назначения\tУзел через который проходит\tКоличество переходов\n");
        for (Map.Entry<Integer, Map<Integer, Integer>> entry : routingData.entrySet()) {
            int destination = entry.getKey();
            for (Map.Entry<Integer, Integer> subEntry : entry.getValue().entrySet()) {
                sb.append(destination).append("\t\t").append(subEntry.getKey()).append("\t\t").append(subEntry.getValue()).append("\n");
            }
        }
        return sb.toString();
    }

    public Map<Integer, Map<Integer, Integer>> getRoutingData() {
        return routingData;
    }
}