package edu.berkeley.nlp.assignments.parsing.student;

import edu.berkeley.nlp.assignments.parsing.TreeAnnotations;
import edu.berkeley.nlp.ling.Tree;
import edu.berkeley.nlp.ling.Trees;

import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Main {
    public static void main(String[] args) {
//        Tree<String> goldTree = (new Trees.PennTreeReader(new StringReader("(ROOT (S (NP (DT the) (ADJP (JJ smelly)) (ADJP (JJ yellow)) (ADJP (JJ rusty)) (NN can)) (VP (VBD fell))))"))).next();
        Tree<String> goldTree = (new Trees.PennTreeReader(new StringReader("(ROOT(S(NP (NNP BSN))(ADVP (RB currently))(VP (VBZ has)(NP(NP(QP (CD 4.6) (CD million))(JJ common) (JJ failed) (NNS shares))(ADJP (JJ outstanding))))(. .)))"))).next();
//        Tree<String> goldTree = (new Trees.PennTreeReader(new StringReader("(ROOT(S (`` ``)(S(VP (TO To)(VP (VB maintain)(NP (DT that) (NN dialogue)))))(VP (VBZ is)(ADJP (RB absolutely) (JJ crucial)))(. .)))"))).next();
//        Tree<String> goldTree = (new Trees.PennTreeReader(new StringReader("(ROOT(S(PP (IN At)(NP (NNP Applied)))(, ,)(NP (NNP Mr.) (NNP Sim))(VP (VBD set) (NP (NN growth)) (PP (IN as) (NP (PRP$ his) (JJ first) (NN objective))))(. .)))"))).next();






        System.out.print(Trees.PennTreeRenderer.render(goldTree));
        List<Tree<String>> trees = Collections.singletonList(goldTree);
        GenerativeParser gp = new GenerativeParser(trees);
        trees = gp.annotateTrees(trees);
        System.out.println("\nAnnotated trees: ");
        for(Tree<String> tree : trees) {
            System.out.print(Trees.PennTreeRenderer.render(tree));
        }
//        Tree<String> unAnnotatedTree = TreeAnnotations.unAnnotateTree(trees.get(0));
//        System.out.print(Trees.PennTreeRenderer.render(unAnnotatedTree));
        String test = "BSN currently has 4.6 million common shares outstanding . ";
//        String test = "To maintain that dialogue is absolutely crucial . ";
        String[] testArray = test.split(" ");
        Tree<String> bestParse = gp.getBestParse(Arrays.asList(testArray));
//        Tree<String> bestParse = gp.getBestParse(Arrays.asList("the", "dog", "runs"));
        System.out.print(Trees.PennTreeRenderer.render(bestParse));
    }
}


/*
Goal parse with h=2 and v=0 for "The smelly yellow rusty can fell".
(ROOT
  (S
    (NP (DT the)
        (@NP->_DT
	  (ADJP (JJ smelly))
	  (@NP->_DT_ADJP
	     (ADJP (JJ yellow))
	     (@NP->_ADJP_ADJP
		(ADJP (JJ rusty))
		(@NP->_ADJP_ADJP
		   (NN can)))
     (@S->_NP
        (VP (VBD fell)))))
 */

/*
Goal parse with h=2 and v=1 for "The smelly yellow rusty can fell".
(ROOT^NULL
  (S^ROOT
    (NP^S (DT^NP the)
        (@NP^S->_DT
	  (ADJP^NP (JJ^ADJP smelly))
	  (@NP^S->_DT_ADJP
	     (ADJP^NP (JJ^ADJP yellow))
	     (@NP^S->_ADJP_ADJP
		(ADJP^NP (JJ^ADJP rusty))
		(@NP^S->_ADJP_ADJP (NN can)))
     (@S^ROOT->_NP
        (VP^S (VBD^VP fell)))))
 */