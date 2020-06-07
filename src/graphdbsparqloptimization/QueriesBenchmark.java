/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package graphdbsparqloptimization;

import java.util.ArrayList;
import java.util.List;
import sparqloptimization.TriplePattern;


public class QueriesBenchmark {
    public static List<TriplePattern> getQuery(int id){
        List<TriplePattern> query = new ArrayList<>();
        switch(id){
            
            case 1://Query 1:
                query.add(new TriplePattern("?e ub:memberOf ?d"));  
                query.add(new TriplePattern("?p ub:worksFor ?d"));
                query.add(new TriplePattern("?p ub:teacherOf ?c"));
                query.add(new TriplePattern("?c ub:name ?C"));
                query.add(new TriplePattern("?e ub:teachingAssistantOf ?c"));
                query.add(new TriplePattern("?e ub:advisor ?p"));
                query.add(new TriplePattern("?a ub:publicationAuthor ?p"));
                query.add(new TriplePattern("?a ub:publicationAuthor ?e"));
                query.add(new TriplePattern("?a ub:name ?A"));                
                query.add(new TriplePattern("?e ub:name ?E"));
                query.add(new TriplePattern("?e ub:undergraduateDegreeFrom ?D"));
                query.add(new TriplePattern("?d ub:subOrganizationOf ?K"));
                break;  
                
            /*case 2:
                query.add(new TriplePattern("?X ub:name ?Y1"));
                query.add(new TriplePattern("?Y ub:name ?Z"));                
                query.add(new TriplePattern("?X ub:emailAddress ?Y2"));
                query.add(new TriplePattern("?X ub:teacherOf ?C"));
                query.add(new TriplePattern("?P ub:publicationAuthor ?Y"));
                query.add(new TriplePattern("?X ub:telephone ?Y3"));                
                query.add(new TriplePattern("?X ub:worksFor <http://www.Department0.University0.edu>"));            
                query.add(new TriplePattern("?Y ub:takesCourse ?C"));                
                query.add(new TriplePattern("?Y ub:memberOf ?D"));
                query.add(new TriplePattern("?Y ub:advisor ?X"));
                query.add(new TriplePattern("?P ub:publicationAuthor ?X"));
                break;
                
            case 3:
                query.add(new TriplePattern("?x ub:publicationAuthor ?gs"));            
                query.add(new TriplePattern("?p ub:teacherOf ?c"));
                query.add(new TriplePattern("?p ub:worksFor ?d"));                
                query.add(new TriplePattern("?p ub:name ?N"));
                query.add(new TriplePattern("?gs ub:advisor ?p"));                
                query.add(new TriplePattern("?c ub:name 'Course40'"));
                query.add(new TriplePattern("?d ub:name 'Department0'"));
                break;*/
                
//            case 4:
//                query.add(new TriplePattern("?X ub:name ?Y1"));
//                query.add(new TriplePattern("?Y ub:name ?Z"));                
//                query.add(new TriplePattern("?X ub:emailAddress ?Y2"));
//                query.add(new TriplePattern("?X ub:teacherOf ?C"));
//                query.add(new TriplePattern("?P ub:publicationAuthor ?Y"));
//                query.add(new TriplePattern("?X ub:telephone ?Y3"));                
//                query.add(new TriplePattern("?X ub:worksFor <http://www.Department0.University0.edu>"));            
//                query.add(new TriplePattern("?Y ub:takesCourse ?C"));                
//                query.add(new TriplePattern("?Y ub:memberOf ?D"));
//                query.add(new TriplePattern("?Y ub:advisor ?X"));
//                query.add(new TriplePattern("?P ub:publicationAuthor ?X"));
//                query.add(new TriplePattern("?Y ub:teachingAssistantOf ?C2"));
//                query.add(new TriplePattern("?C2 ub:name ?N"));
//
//                break;
        }
        return query;
    }
    
    
    public static List<TriplePattern> getQueriesForTestingCardinalities(int id){
        List<TriplePattern> query = new ArrayList<>();
        switch(id){
            
            case 1://Query 2:
                //This query increases in complexity: 3 classes and 3 properties are involved. Additionally,
                //there is a triangular pattern of relationships between the objects involved.
                
                //query.add(new TriplePattern("?X rdf:type ub:GraduateStudent"));
                //query.add(new TriplePattern("?Y rdf:type ub:University"));
                //query.add(new TriplePattern("?Z rdf:type ub:Department"));  
                query.add(new TriplePattern("?X ub:memberOf ?Z"));
                query.add(new TriplePattern("?Z ub:subOrganizationOf ?Y"));                
                query.add(new TriplePattern("?X ub:undergraduateDegreeFrom ?Y"));   
                
                break;  

                
                
//            case 2://Query 3:
//                //This query is similar to Query 1 but class Publication has a wide hierarchy.                
//                query.add(new TriplePattern("?X rdf:type ub:Publication"));
//                query.add(new TriplePattern("?X ub:publicationAuthor <http://www.Department0.University0.edu/AssistantProfessor0>"));
//                
//                break;  


            case 2://Query 4:
            //This query has small input and high selectivity. 
            //It assumes subClassOf relationship between Professor and its subclasses. Class Professor has a wide hierarchy.  
            //Another feature is that it queries about multiple properties of a single class.            
                
            //query.add(new TriplePattern("?X rdf:type ub:FullProfessor"));
            query.add(new TriplePattern("?X ub:worksFor <http://www.Department0.University0.edu>"));            
            query.add(new TriplePattern("?X ub:name ?Y1"));
            query.add(new TriplePattern("?X ub:emailAddress ?Y2"));
            query.add(new TriplePattern("?X ub:telephone ?Y3"));
                
                break;  

//            case 4://Query 7:
//            //This query is similar to Query 6 in terms of class Student  
//            //but it increases in the number of classes and properties and its selectivity is high 
//                
//            query.add(new TriplePattern("?X rdf:type ub:UndergraduateStudent"));
//            query.add(new TriplePattern("?Y rdf:type ub:Course"));
//            query.add(new TriplePattern("?X ub:takesCourse ?Y"));
//            query.add(new TriplePattern("<http://www.Department0.University0.edu/AssociateProfessor0> ub:teacherOf ?Y"));
//
//                break;
//                
//                
//            case 5://Query 8:
//            //This query is further more complex than Query 7 by including one more property.             
//            query.add(new TriplePattern("?X rdf:type ub:UndergraduateStudent"));
//            query.add(new TriplePattern("?Y rdf:type ub:Department"));
//            query.add(new TriplePattern("?X ub:memberOf ?Y"));
//            query.add(new TriplePattern("?Y ub:subOrganizationOf <http://www.University0.edu>"));
//            query.add(new TriplePattern("?X ub:emailAddress ?Z"));
//            query.add(new TriplePattern("?Y rdf:type ub:Department"));
//
//                break;
//                
//            case 7://Query 9:
//            //Besides the aforementioned features of class Student and the wide hierarchy of class Faculty, 
//            //like Query 2, this query is characterized by the most classes and properties in the query set and  
//            //there is a triangular pattern of relationships.
//                
//            query.add(new TriplePattern("?X rdf:type ub:UndergraduateStudent"));
//            query.add(new TriplePattern("?Y rdf:type ub:FullProfessor"));
//            query.add(new TriplePattern("?Z rdf:type ub:Course"));
//            query.add(new TriplePattern("?X ub:advisor ?Y"));
//            query.add(new TriplePattern("?Y ub:teacherOf ?Z"));
//            query.add(new TriplePattern("?X ub:takesCourse ?Z"));
//   
//                break;
//
//            case 8://Query 10:
//            //This query differs from Query 6, 7, 8 and 9 in that it only requires the (implicit) subClassOf relationship between GraduateStudent and Student, 
//             //i.e., subClassOf relationship between UndergraduateStudent and Student does not add to the results.                
//            query.add(new TriplePattern("?X rdf:type ub:GraduateStudent"));
//            query.add(new TriplePattern("?X ub:takesCourse <http://www.Department0.University0.edu/GraduateCourse0>"));
//
//                break;
        }
        return query;
    }
}
