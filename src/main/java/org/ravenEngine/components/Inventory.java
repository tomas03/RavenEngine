package org.ravenEngine.components;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class Inventory extends Component {
    private final Map<String,Integer> items = new LinkedHashMap<>();

    public Inventory(){

    }

    public void addItem(String itemId,int amount){
        if (amount<=0)return;
        int current = items.getOrDefault(itemId,0);
        items.put(itemId,current+amount);
        System.out.println("[Inventario] Añadido: "+itemId+" x"+amount+" (Total: "+items.get(itemId)+")");
    }

    public boolean removeItem(String itemId, int amount){
        if (!items.containsKey(itemId)||items.get(itemId)<amount){
            return false;
        }
        int remaining = items.get(itemId) - amount;
        if (remaining <= 0){
            items.remove(itemId);
        } else{
            items.put(itemId,remaining);
        }
        return true;
    }

    public int getItemCount(String itemId){
        return items.getOrDefault(itemId, 0);
    }

    public Map<String,Integer> getItems(){
        return Collections.unmodifiableMap(items);
    }

    public void clear(){
        items.clear();
    }
}
