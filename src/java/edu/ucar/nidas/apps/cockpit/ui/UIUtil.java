package edu.ucar.nidas.apps.cockpit.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import edu.ucar.nidas.model.Var;

public class UIUtil {

    public List<Var> getSortedVarsByHeight(List<Var> invars) { // sort by  height
        //System.out.println("\ngetSortedVarsByHeight");
        if (invars == null || invars.size()<1 ) return null;
       
        ArrayList<Float> hs = new ArrayList<Float>(); // heights
        ArrayList<String> namesHeight = new ArrayList<String>(); // names with height
        ArrayList<Float> cmhs = new ArrayList<Float>(); // cm heights
        ArrayList<String> namesCMHeight = new ArrayList<String>(); // names with cm height
        ArrayList<Var> noHeightVars = new ArrayList<Var>(); // vars without height 
        HashMap<String, Var> nameToVarHeight = new HashMap<String, Var>(); //name-var map

        // sort height
        //System.out.println("\n Sort Height");
        int len = invars.size();
        for (int i = 0; i < len; i++) {
            Var var = invars.get(i);
            String vname = var.getName();
            String h = isHeight(vname);
            if (h != null) {
               // System.out.println("\n h="+h);
                if (h.endsWith("cm"))  { //cm
                    h = h.substring(0, (h.length()-2));
                    System.out.println("\nnew cmh="+h);
                    Float nt = new Float(h);
                    if (!cmhs.contains(nt)) {
                        cmhs.add(nt);
                    }
                    namesCMHeight.add(vname);
                   // System.out.println("\nnew-cmname="+vname);
                } else if (h.endsWith("m")) {
                    h = h.substring(0, (h.length()-1));
                   // System.out.println("\nnew h="+h);
                    Float nt = new Float(h);
                    if (!hs.contains(nt)) {
                        hs.add(nt);
                        //System.out.println("\nnewht="+nt);
                    }
                    namesHeight.add(vname);
                  //  System.out.println("\nnew-name="+vname);

                } else { //unknown
                   // System.out.println("\n unknown height="+h);
                }
                nameToVarHeight.put(vname, var);
            } else {
                noHeightVars.add(var);
                //System.out.println("\nno-height-var="+vname);
            }
        }
        Collections.sort(hs);
        Collections.sort(cmhs);

        // sort names with cm-height
        List<String> all = new ArrayList<String>();
       // System.out.println("\nSorted names with cm-Height");
        for (int i = cmhs.size()-1; i >=0; i--) {
            String oneH = cmhs.get(i)+""; //pure height
            if (oneH.endsWith(".0")) oneH = oneH.substring(0, oneH.length()-2); // erase .0
            List<String> names = new ArrayList<String>(); // names with height
            for (int j = 0; j < namesCMHeight.size(); j++) {
                String nm = namesCMHeight.get(j);
                if (nm.indexOf(oneH+"cm") != -1  ) {
                    names.add(nm);
                    namesCMHeight.remove(nm);
                    j--;
                }
            }

            Collections.sort(names, new AlphabeticComparator());
            all.addAll(0,names);
            //System.out.println("\nSorted names with -cmheight="+all.toArray().toString() );
            names.clear();
        }

        // sort names with height
        //System.out.println("\nSorted names with Height");
        for (int i = hs.size()-1; i>=0; i--) {
            String oneH = hs.get(i)+""; //pure height
            if (oneH.endsWith(".0")) oneH = oneH.substring(0, oneH.length()-2); // erase .0
            List<String> names = new ArrayList<String>(); // names with height
            for (int j = 0; j < namesHeight.size(); j++) {
                String nm = namesHeight.get(j);
                if (nm.indexOf(oneH+"m") != -1  ) {
                    names.add(nm);
                    namesHeight.remove(nm);
                    j--;
                }
            }

            Collections.sort(names, new AlphabeticComparator());
            if (names!=null && names.size()>0) all.addAll(0,names);
           // System.out.println("\nSorted names with height="+all.toArray().toString() );
            names.clear();
            //all.add(names);
        }
        
         
        // sort vars with height
       // vars.clear();
        List<Var> vars = new ArrayList<Var>();
        for (int i = 0; i < all.size(); i++) {
            Var var = nameToVarHeight.get(all.get(i));
            if (var !=null ) vars.add(var);
        }
        // sort no-height gauges by VarName
        List<Var> nhvars = getSortedVars(noHeightVars);
        if (nhvars != null && nhvars.size()>0) vars.addAll(nhvars);
        /*for (int i=0; i< vars.size(); i++) {
            System.out.println("\nsorted var="+vars.get(i).getName());
        }*/

        hs.clear();
        cmhs.clear();
        namesHeight.clear();
        namesCMHeight.clear();
        noHeightVars.clear();
        nameToVarHeight.clear();
        return vars;

    }

    //Object[] nm=null;
    public List<Var> getSortedVars(List<Var> invars) {
       // System.out.println("\ngetSortedVars="+invars.toArray().toString() );
        if (invars == null || invars.size()<1) return null;
        List<Var> vars = new ArrayList<Var>();
        List<String> names = new ArrayList<String>(); // names
        HashMap<String, Var> nameToVar = new HashMap<String, Var>();

        for (int i = 0; i < invars.size(); i++) {
            Var var = invars.get(i);
            String name = var.getName();
           // System.out.println("\nunSorted var =" +name );
            names.add(name);
            nameToVar.put(name, var);
        }
        Collections.sort(names, (new AlphabeticComparator()));

        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            //System.out.println("\nSorted var =" +name );
            Var var = nameToVar.get(name);
            if (var != null) {
                vars.add(var);
               // System.out.println("\nSorted var =" +name );
            }
        }
        nameToVar.clear();
        names.clear();
        return vars;
    }

    public String isHeight(String name) {
        if (name==null ) return null;

        String sub = "";
        boolean dig = true;
        for (int j=0; j<name.length(); j++){
            char c = name.charAt(j);
            if (!Character.isDigit(c)) {
                if (String.valueOf(c).equals(".") && j > 0 && Character.isDigit(name.charAt(j-1))) { sub += c;}
                else if (String.valueOf(c).equals("m") && j>0 && Character.isDigit(name.charAt(j-1))) {sub += c; break;}   //dig+m
                else if (String.valueOf(c).equals("c") && j>0 && Character.isDigit(name.charAt(j-1)) &&(j+1)<name.length() && String.valueOf(name.charAt(j+1)).equals("m")) {sub += "cm"; break;} //dig+cm
            } else sub += c;
        }
        if (sub.length()<=0) {return null;}
        if (sub.endsWith("m") || sub.endsWith("cm")) {return sub;}
        else return null;

    }



    public ArrayList<Gauge> getSortedGaugesByHeight( ArrayList<Gauge> ggs) { // sort by height
       // System.out.println("\ngetSortedGaugesByHeight");
        if (ggs == null || ggs.size()<1) return null;
        ArrayList<Gauge> gs = new ArrayList<Gauge>();
        ArrayList<Var> lvars = new ArrayList<Var>(); 
        HashMap<Var, Gauge> varToGaugeHeight = new HashMap<Var,Gauge>();

        for (int i=0; i< ggs.size(); i++) {
            Gauge g= ggs.get(i);
            Var var= g.getVar();
            String name = var.getName();
           // System.out.println("\n"+i+" getSortedGaugesByHeight-bf-sort g-name ="+name );
            if (!lvars.contains(var)){
                lvars.add(var);
                varToGaugeHeight.put(var,g);
            }
            //if (!nameToGaugeHeight.containsKey(var.getName()))  nameToGaugeHeight.put(var.getName(), g);
        }

       // System.out.println("vars-len-bf="+lvars.size());
        List<Var> vars = getSortedVarsByHeight(lvars);
       // System.out.println("vars-len-after="+vars.size());

        for (int i=0; i<vars.size(); i++) {
            Var var = vars.get(i);
       //     System.out.println("\ngetSortedGaugesByHeight-after-sort g-name ="+name );
            Gauge ng = varToGaugeHeight.get(var);
            if (ng != null ) {
               // System.out.println("\n"+i+" getSortedGaugesByHeight-after-sort g-label ="+ng._label );
                gs.add(ng);            
            }
        }

        return gs;
    }


    public ArrayList<Var> getSortedVarsByHeight( ArrayList<Gauge> ggs) { //
        List<Var> lvars = new ArrayList<Var>(); 

        for (int i=0; i< ggs.size(); i++) {
            Gauge g= ggs.get(i);
            if (!lvars.contains(g.getVar())) lvars.add(g.getVar());
        }

        return (ArrayList<Var>)getSortedVarsByHeight(lvars);
    }


    public ArrayList<Gauge> getSortedGauges( ArrayList<Gauge> ggs) { // sort by vars
        int glen= ggs.size();
        ArrayList<Gauge> gs = new ArrayList<Gauge>();
        List<String> names = new ArrayList<String>(); //names
        HashMap<String, Gauge> nameToGauge = new HashMap<String,Gauge>();
        for (int i=0; i<glen;i++) {
            Gauge g = ggs.get(i);
            names.add(g._label);
            nameToGauge.put(g._label, g);
        }

        Collections.sort(names, (new AlphabeticComparator()));
        for (int i=0; i<names.size();i++) {
            Gauge g=nameToGauge.get(names.get(i));
            gs.add(g);
        }
        nameToGauge.clear();
        return gs;
    }


    public ArrayList<Var> getSortedVars( ArrayList<Gauge> ggs) {
        int glen= ggs.size();
        ArrayList<Var> vars = new ArrayList<Var>();
        List<String> names = new ArrayList<String>(); //names
        HashMap<String, Var> nameToVar = new HashMap<String,Var>();
        for (int i=0; i<glen;i++) {
            Gauge g = ggs.get(i);
            names.add(g._label);
            nameToVar.put(g._label, g._var);
        }

        Collections.sort(names, new AlphabeticComparator());
        for (int i=0; i<names.size();i++) {
            Var var=nameToVar.get(names.get(i));
            vars.add(var);
        }
        nameToVar.clear();
        return vars;
    }

    class AlphabeticComparator implements Comparator<String> {
        public int compare(String s1, String s2) {
            return s1.compareToIgnoreCase(s2);
        }
    }

}
