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
        sb.append("Текущая точка\tЧерез точку\tДлина пути\n");
        for (Map.Entry<Integer, Map<Integer, Integer>> entry : routingData.entrySet()) {
            int currentNode = entry.getKey();
            for (Map.Entry<Integer, Integer> subEntry : entry.getValue().entrySet()) {
                int viaNode = subEntry.getKey();
                int pathLength = subEntry.getValue();
                sb.append(currentNode).append("\t\t").append(viaNode).append("\t\t").append(pathLength).append("\n");
            }
        }
        return sb.toString();
    }


    public Map<Integer, Map<Integer, Integer>> getRoutingData() {
        return routingData;
    }
}