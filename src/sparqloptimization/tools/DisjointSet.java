/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sparqloptimization.tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class DisjointSet <T> {
    HashMap<T, Integer> indices = new HashMap<>();
    List<T> items;
    int[] parents;
    public DisjointSet(Collection<T> c){
        items =new ArrayList<T>();
        int index = 0;
        for(T item : c){
            indices.put(item, index++);
            items.add(item);
        }
        parents = new int[items.size()];
        for(int i=0; i<parents.length; i++)
            parents[i] = -1;
    }
    
    int __GetParent (int itemIndex){
        if (parents[itemIndex] == -1)
            return itemIndex;
        return parents[itemIndex] = __GetParent(parents[itemIndex]);
    }
    
    public void join(T item1, T item2){
        int index1 = indices.get(item1);
        int index2 = indices.get(item2);
        int parent1 = __GetParent(index1);
        int parent2 = __GetParent(index2);
        if (parent1 != parent2)
            parents[parent1] = parent2;
    }
    
    public T representative(T item){
        int index = indices.get(item);
        return items.get(__GetParent(index));
    }
}
