package edu.berkeley.nlp.assignments.parsing.student;

import edu.berkeley.nlp.ling.Tree;
import edu.berkeley.nlp.ling.Trees;
import edu.berkeley.nlp.util.Filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MyTreeAnnotations {
    public static Tree<String> annotateTree(Tree<String> unAnnotatedTree) {
        return binarizeTreeWithV2(unAnnotatedTree, "NULL");
    }

    private static Tree<String> binarizeTree(Tree<String> tree) {
        String label = tree.getLabel();
//        String parentLabel = tree.getParent();
        //[NP [N]]
        if(tree.isLeaf()) return new Tree<String>(label);
        //[S[NP[N ...]]]
        if(tree.getChildren().size() ==1) {
            if(tree.getChildren().get(0).getLabel().equals("DT")) {
                tree.getChildren().get(0).setLabel("UNARY-DT");
//                Tree<String> childTree = tree.getChildren().get(0);
//                childTree.setLabel("UNARY-DT");
//                return new Tree<>(label+"-Unary", Collections.singletonList(binarizeTree(childTree)));
            }
            else if (tree.getChildren().get(0).getLabel().equals("RB")) {
                tree.getChildren().get(0).setLabel("UNARY-RB");
//                Tree<String> childTree = tree.getChildren().get(0);
//                childTree.setLabel("UNARY-RB");
//                return new Tree<>(label+"-Unary", Collections.singletonList(binarizeTree(childTree)));
            }
            return new Tree<> (label, Collections.singletonList(binarizeTree(tree.getChildren().get(0))));
//            return new Tree<> (label+"-Unary", Collections.singletonList(binarizeTree(tree.getChildren().get(0))));
        }
        String intermediateLabel = "@" + label /*+ "^" + parentLabel*/ + "->";
        Tree<String> intermediateTree = binarizeTreeHelper(tree, 0, intermediateLabel);
        return new Tree<>(label, intermediateTree.getChildren());
    }

    private static Tree<String> binarizeTreeWithV2(Tree<String> tree, String oldLabel) {
        String label = tree.getLabel();
//        if(label.contains("-")) {
//            if(!label.equals("NP-TMP")) {
//                label = label.split("-")[0];
//                tree.setLabel(label);
//                System.out.println("New label without tag: " + label);
//            }
//        }
        if(label.equals("am") || label.equals("was") || label.equals("are") || label.equals("is") || label.equals("were") || label.equals("been") || label.equals("being")) {
            label = label + "~BE";
        }
        if(label.equals("have") || label.equals("has") || label.equals("had") || label.equals("having")) {
            label = label + "~HAVE";
        }

        boolean dominatesV = false;
        boolean allPreterminals = true;
        boolean rightRecNP = false;
        boolean conjB = false;
        boolean conjA = false;
        boolean subConj = false;
        boolean comp = false;
        boolean pp = false;
        boolean nm = false;

        List<Tree<String>> children = tree.getChildren();
        if(!children.isEmpty()) {
            if (children.get(children.size() - 1).getLabel().equals("NP") && label.equals("NP")) {
                rightRecNP = true;
            }
        }

        for(Tree<String> child : children) {
            String childLabel = child.getLabel();
            if(childLabel.equals("MD") || childLabel.charAt(0)=='V') {
                dominatesV = true;
            }
            if(!child.isLeaf()) {
                allPreterminals = false;
            }
            if((childLabel.equals("But") || childLabel.equals("but")) && label.equals("CC")) {
                conjB = true;
            }
            if((childLabel.equals("&")) && (label.equals("CC"))) {
                conjA = true;
            }
            if(childLabel.equals("while") || childLabel.equals("While") || childLabel.equals("as") || childLabel.equals("As") || childLabel.equals("if") || childLabel.equals("If")) {
                subConj = true;
            }
            if(childLabel.equals("that") || childLabel.equals("That") || childLabel.equals("for") || childLabel.equals("For")) {
                comp = true;
            }
            if(childLabel.equals("of") || childLabel.equals("Of") || childLabel.equals("in") || childLabel.equals("In") || childLabel.equals("from") || childLabel.equals("From") || childLabel.equals("about") || childLabel.equals("About") || childLabel.equals("with") || childLabel.equals("With") || childLabel.equals("up") || childLabel.equals("Up")) {
                pp = true;
            }
            if(childLabel.equals("by") || childLabel.equals("By")) {
                nm = true;
            }
        }
        if(dominatesV) {
            label = label + "-V";
        }
        if(conjB) {
            label = label + "-B";
        }
        if(conjA) {
            label = label + "-&";
        }
        if(subConj) {
            label = label + "-SC";
        }
        if(comp) {
            label = label + "-CMP";
        }
        if(pp) {
            label = label + "-PP";
        }
        if(nm) {
            label = label + "-NM";
        }
        if(label.contains("NP")) {
            if(allPreterminals) {
                label = label + "-B";
            }
            if(rightRecNP) {
                label = label + "-RRNP";
            }
        }
        if(tree.isLeaf()) return new Tree<>(label);
        if(tree.getChildren().size()==1) {
            if(tree.getChildren().get(0).getLabel().equals("DT")) {
                tree.getChildren().get(0).setLabel("UNARY-DT");
//                Tree<String> childTree = tree.getChildren().get(0);
//                childTree.setLabel("UNARY-DT");
//                return new Tree<>(label, Collections.singletonList((binarizeTreeWithV2(childTree, label))));
            }
            else if (tree.getChildren().get(0).getLabel().equals("RB")) {
                tree.getChildren().get(0).setLabel("UNARY-RB");
//                Tree<String> childTree = tree.getChildren().get(0);
//                childTree.setLabel("UNARY-RB");
//                return new Tree<>(label, Collections.singletonList(binarizeTreeWithV2(childTree, label)));
            }
            else if (tree.getChildren().get(0).getLabel().equals("%")) {
                label = "%";
            }
            return new Tree<> (label+"^"+oldLabel, Collections.singletonList(binarizeTreeWithV2(tree.getChildren().get(0), label)));
        }
        if(tree.getChildren().size()==2) {
            return new Tree<>(label+"^"+oldLabel, Arrays.asList(binarizeTreeWithV2(tree.getChildren().get(0), label), binarizeTreeWithV2(tree.getChildren().get(1), label)));
        }
        String intermediateLabel = "@" + label + "->";
        Tree<String> intermediateTree = binarizeTreeHelperWithV2(tree, 0, intermediateLabel, oldLabel);
        return new Tree<>(label+"^"+oldLabel, intermediateTree.getChildren());
    }

    private static Tree<String> binarizeTreeHelperWithV2(Tree<String> tree, int numChildrenGenerated, String intermediateLabel, String oldLabel) {
        Tree<String> leftTree = tree.getChildren().get(numChildrenGenerated);
        List<Tree<String>> children = new ArrayList<>();
        children.add(binarizeTreeWithV2(leftTree, tree.getLabel()));
        String newIntermediateLabel = "";
        //do vertical context
        if(!intermediateLabel.contains("^")) {
            if (intermediateLabel.contains("-")) {
                String[] contextCheck = intermediateLabel.split("-");
                if (contextCheck.length == 2) {
                    newIntermediateLabel = contextCheck[0] + "^" + oldLabel + "-" + contextCheck[1];
                }
            } else {
                newIntermediateLabel = intermediateLabel + "^" + oldLabel;
            }
        }

        //do horizontal context
        //if there are more siblings to the right
        if (numChildrenGenerated < tree.getChildren().size() -1) {
            //if we did not need to add vertical context, so newIntermediateLabel is still empty:
            if(newIntermediateLabel.isEmpty()) {
                //if it is not the leftmost sibling
                if(intermediateLabel.contains("_")) {
                    String[] contextCheck = intermediateLabel.split("_");
                    //if we've already seen 2 siblings
                    if(contextCheck.length == 3) {
                        newIntermediateLabel = contextCheck[0]+"_"+contextCheck[contextCheck.length-1]+"_"+leftTree.getLabel();
                    } else {
                        newIntermediateLabel = intermediateLabel+"_"+leftTree.getLabel();
                    }
                } else {
                    newIntermediateLabel = intermediateLabel+"_"+leftTree.getLabel();
                }
            } else {
                //if it is not the leftmost sibling
                if(newIntermediateLabel.contains("_")) {
                    String[] contextCheck = newIntermediateLabel.split("_");
                    //if we've already seen 2 siblings
                    if(contextCheck.length == 3) {
                        newIntermediateLabel = contextCheck[0]+"_"+contextCheck[contextCheck.length-1]+"_"+leftTree.getLabel();
                    } else {
                        newIntermediateLabel = newIntermediateLabel+"_"+leftTree.getLabel();
                    }
                } else {
                    newIntermediateLabel = newIntermediateLabel+"_"+leftTree.getLabel();
                }
            }
            Tree<String> rightTree = binarizeTreeHelperWithV2(tree, numChildrenGenerated + 1, newIntermediateLabel, oldLabel);
            children.add(rightTree);
        }
        return new Tree<>(intermediateLabel, children);
    }

    private static Tree<String> binarizeTreeHelper(Tree<String> tree, int numChildrenGenerated, String intermediateLabel) {
        Tree<String> leftTree = tree.getChildren().get(numChildrenGenerated);
        List<Tree<String>> children = new ArrayList<>();
        children.add(binarizeTree(leftTree));

        //if there are more siblings to the right
        if (numChildrenGenerated < tree.getChildren().size() -1) {
            String newIntermediateLabel = "";
            //if it is not the leftmost sibling
            if(intermediateLabel.contains("_")) {
                String[] contextCheck = intermediateLabel.split("_");
                //if we've already seen 2 siblings
                if(contextCheck.length == 3) {
                    newIntermediateLabel = contextCheck[0]+"_"+contextCheck[contextCheck.length-1]+"_"+leftTree.getLabel();
                } else {
                    newIntermediateLabel = intermediateLabel+"_"+leftTree.getLabel();
                }
            } else {
                newIntermediateLabel = intermediateLabel+"_"+leftTree.getLabel();
            }
            Tree<String> rightTree = binarizeTreeHelper(tree, numChildrenGenerated + 1, newIntermediateLabel);
            children.add(rightTree);
        }
        return new Tree<>(intermediateLabel, children);
    }

    public static Tree<String> unAnnotateTree(Tree<String> annotatedTree) {
        Tree<String> debinarizedTree = Trees.spliceNodes(annotatedTree, new Filter<String>()
        {
            public boolean accept(String s) {
                return s.startsWith("@");
            }
        });
        Tree<String> unAnnotatedTree = (new LabelNormalizer()).transformTree(debinarizedTree);
        return unAnnotatedTree;
    }

    public static class LabelNormalizer implements Trees.TreeTransformer<String>
    {
        public static String transformLabel(Tree<String> tree) {
            String transformedLabel = tree.getLabel();
            if(transformedLabel.contains("UNARY-DT")) {transformedLabel = transformedLabel.replace("UNARY-", "");} //works
            if(transformedLabel.contains("UNARY-RB")) {transformedLabel = transformedLabel.replace("UNARY-", "");} //works
            if(transformedLabel.contains("NP-B")) {transformedLabel = transformedLabel.replace("-B", "");} //works
            if(transformedLabel.contains("NP-RRNP")) {transformedLabel = transformedLabel.replace("-RRNP", "");} //works
            if(transformedLabel.contains("-V")) {transformedLabel = transformedLabel.replace("-V", "");} //works
            if(transformedLabel.contains("-SC")) {transformedLabel = transformedLabel.replace("-SC", "");} //works
            if(transformedLabel.contains("-CMP")) {transformedLabel = transformedLabel.replace("-CMP", "");} //works
            if(transformedLabel.contains("-PP")) {transformedLabel = transformedLabel.replace("-PP", "");} //works
            if(transformedLabel.contains("-NM")) {transformedLabel = transformedLabel.replace("-NM", "");} //works
            if(transformedLabel.contains("CC-B")) {transformedLabel = transformedLabel.replace("-B","");} //works
            if(transformedLabel.contains("CC-&")) {transformedLabel = transformedLabel.replace("-&","");} //works
            if(transformedLabel.contains("%")) {transformedLabel = transformedLabel.replace("%","NN");} //works
            if(transformedLabel.contains("~BE")) {transformedLabel=transformedLabel.split("~")[0];} //works
            if(transformedLabel.contains("~HAVE")) {transformedLabel=transformedLabel.split("~")[0];} //works


            if (tree.isLeaf()) return transformedLabel;
            int index = -1;
            for (String delim : new String[] { "=", "<", ">", "^", "_", "->"}) {
                final int currIndex = transformedLabel.indexOf(delim);
                index = index < 0 ? currIndex : (currIndex < 0 ? index : Math.min(currIndex, index));
            }

            transformedLabel = new String(transformedLabel.substring(0, index < 0 ? transformedLabel.length() : index));
            return transformedLabel;
        }

        public Tree<String> transformTree(Tree<String> tree) {
            String transformedLabel = transformLabel(tree);
            if (tree.isLeaf()) return new Tree<String>(transformedLabel);
            List<Tree<String>> transformedChildren = new ArrayList<Tree<String>>();
            for (Tree<String> child : tree.getChildren())
                transformedChildren.add(transformTree(child));
            return new Tree<String>(transformedLabel, transformedChildren);
        }
    }
}
