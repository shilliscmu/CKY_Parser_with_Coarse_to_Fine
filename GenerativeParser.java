package edu.berkeley.nlp.assignments.parsing.student;

import edu.berkeley.nlp.assignments.parsing.*;
import edu.berkeley.nlp.ling.Tree;
import edu.berkeley.nlp.ling.Trees;
import edu.berkeley.nlp.util.Indexer;

import java.util.*;

import static java.lang.System.exit;

public class GenerativeParser implements Parser {
    private SimpleLexicon lexicon;
    private Grammar grammar;
    private Indexer<String> labelIndexer;
    private UnaryClosure uc;
    private short[][][][] binaryBackpointers;
    private short[] binaryBackpointer = {-1, -1, -1};
    private short[][][][] unaryBackpointers;
    private short[] unaryBackpointer = {-1};

    public GenerativeParser(List<Tree<String>> trainTrees) {
        List<Tree<String>> binarizedTrainTrees = annotateTrees(trainTrees);
//        for(Tree<String> binarizedTrainTree : binarizedTrainTrees) {
//            System.out.print(Trees.PennTreeRenderer.render(binarizedTrainTree));
//        }
        grammar = Grammar.generativeGrammarFromTrees(binarizedTrainTrees);
        labelIndexer = grammar.getLabelIndexer();

//        List<BinaryRule> binaryRules = grammar.getBinaryRules();
//        System.out.println("\nBinary Rules: ");
//        for(BinaryRule bR : binaryRules) {
//            System.out.println(labelIndexer.get(bR.parent) + " -> " + labelIndexer.get(bR.leftChild) + " " + labelIndexer.get(bR.rightChild));
//        }
//        System.out.println();
//
//        List<UnaryRule> unaryRules = grammar.getUnaryRules();
//        System.out.println("Unary Rules: ");
//        for(UnaryRule uR : unaryRules) {
//            System.out.println(labelIndexer.get(uR.parent) + " -> " + labelIndexer.get(uR.child));
//        }
//        System.out.println();

        uc = new UnaryClosure(grammar.getLabelIndexer(), grammar.getUnaryRules());
        lexicon = new SimpleLexicon(binarizedTrainTrees);
    }

    public Tree<String> getBestParse(List<String> sentence) {
        short size = (short) sentence.size();
        short labelIndexerSize = (short) labelIndexer.size();
        String[] sentenceArray = sentence.toArray(new String[0]);


        float[][][] binaryChart = new float[size][size+1][labelIndexerSize]; //[min][max][C]
        float[][][] unaryChart = new float[size][size+1][labelIndexerSize]; //[min][max][C]
        binaryBackpointers = new short[size][size+1][labelIndexerSize][3]; //[min][max][C][C1 C2 mid]
        unaryBackpointers = new short[size][size+1][labelIndexerSize][1]; //[min][max][C][C1]

        for(short i = 0; i < size; i++) {
            for(short j = 0; j < size+1; j++) {
                Arrays.fill(binaryChart[i][j], Float.NEGATIVE_INFINITY);
                Arrays.fill(unaryChart[i][j], Float.NEGATIVE_INFINITY);
                for(short k = 0; k < labelIndexerSize; k++) {
                    Arrays.fill(binaryBackpointers[i][j][k], (short)-1);
                    Arrays.fill(unaryBackpointers[i][j][k], (short)-1);
                }
            }
        }


//        binaryBackpointer = new int[3]; //C1 C2 mid
//        unaryBackpointer = new int[1]; //C1

        //preterminal rules
        //for each wi from left to right
        for(short i = 0; i < size; i++) {
            //for each unary rule C-> wi
            String word = sentenceArray[i];
//            String word = sentence.get(i);
            for(String C : lexicon.getAllTags()) {
//            for(UnaryRule u : unaryRules) {
                float score = (float)lexicon.scoreTagging(word, C);
                if(!Float.isNaN(score) && !Float.isInfinite(score) /*&& score>0*/) {
                    binaryChart[i][i + 1][labelIndexer.indexOf(C)] = score;
//                    System.out.println("At span " + i + " to " + (i+1) + " for word " + word + ", we had a score of " + score + "for " + C);
                }
            }
        }
//        //old way of looping by parent
//        for(short i = 0; i < size; i++) {
//            for(String C : labelIndexer) {
//                short parent = (short) labelIndexer.indexOf(C);
//                for(UnaryRule uR : uc.getClosedUnaryRulesByParent(parent)) {
//                    if(binaryChart[i][i+1][uR.getChild()] != Float.NEGATIVE_INFINITY) {
//                        short child = (short)uR.getChild();
//                        float t1 = binaryChart[i][i+1][child];
//                        float score = (float) uR.getScore();
//                        if(!Float.isNaN(score) && !Float.isInfinite(score)) {
//                            unaryChart[i][i+1][parent] = (t1  + score);
//                            unaryBackpointer = new short[]{child};
//                            unaryBackpointers[i][i+1][parent] = unaryBackpointer;
//                        }
//                    }
//                }
//            }
//        }
        //new way of looping by child
        for(short i = 0; i < size; i++) {
            for(String C : labelIndexer) {
                short child = (short) labelIndexer.indexOf(C);
                for(UnaryRule uR : uc.getClosedUnaryRulesByChild(child)) {
                    if(binaryChart[i][i+1][child] != Float.NEGATIVE_INFINITY) {
                        short parent = (short) uR.getParent();
                        float t1 = binaryChart[i][i+1][child];
                        float score = (float) uR.getScore();
                        if(!Float.isNaN(score) && !Float.isInfinite(score)) {
                            unaryChart[i][i+1][parent] = (t1 + score);
                            unaryBackpointer = new short[]{child};
                            unaryBackpointers[i][i+1][parent] = unaryBackpointer;
                        }
                    }
                }
            }
        }

        //binary and unary rules
        //only preterminal rules live at max=1?
        for(short max = 2; max <= size; max++) {
            for(int min = max - 2; min >=0; min--) {
                for (int mid = min + 1; mid <= max - 1; mid++) {
                    short C = -1;
                    binaryBackpointer = new short[]{-1,-1,-1};
                    float[] allC1s = unaryChart[min][mid];
//                    System.out.println("For min " + min + " and max " + max + " allC1s array is: ");
//                    System.out.println(Arrays.toString(allC1s));
                    for(short C1 = 1; C1 < allC1s.length; C1++) {
                        if(allC1s[C1] != Float.NEGATIVE_INFINITY) {
//                            System.out.println("Possible C1 at min = " + min + " and mid = " + mid + ": " + labelIndexer.get(C1));
//                            System.out.println("Value at C1 is: " + allC1s[C1]);
                            //for each binary rule C -> C1 C2
                            for (BinaryRule bR : grammar.getBinaryRulesByLeftChild(C1)) {
                                float t1 = 0;
                                float t2 = 0;
                                float candidate = 0;
                                if (unaryChart[min][mid][C1] != Float.NEGATIVE_INFINITY) {
                                    t1 = unaryChart[min][mid][C1];
                                    if (unaryChart[mid][max][bR.getRightChild()] != Float.NEGATIVE_INFINITY) {
                                        short C2 = (short) bR.getRightChild();
                                        t2 = unaryChart[mid][max][C2];
                                        double score = (float) bR.getScore();
                                        if (!Double.isNaN(score) && !Double.isInfinite(score)) {
                                            float t = (t1 + t2);
                                            candidate = (float)(t + score);
                                            C = (short) bR.getParent();
                                            if (candidate > binaryChart[min][max][C]) {
                                                binaryChart[min][max][C] = candidate;
                                                binaryBackpointer = new short[]{C1, C2, (short) mid};
    //                                            System.out.println("In the binary chart from " + min + " to " + max + " for parent " + labelIndexer.get(C) + " the best children are " + labelIndexer.get(binaryBackpointer[0]) + " and " + labelIndexer.get(binaryBackpointer[1]) + " and the mid is " + binaryBackpointer[2] + " with a score of " + best);
    //                                            binaryChart[min][max][C] = best;
                                                binaryBackpointers[min][max][C] = binaryBackpointer;
                                            }
                                        }
                                    }
                                }
                            }
//                            if(C != 0 && binaryBackpointer[0] != 0 && binaryBackpointer[1] != 0) {

//                            }
                        }
                    }
//                    System.out.println("Done checking possible C1s for that span.\n");
//                    //for each syntactic category C
//                    for (String C : labelIndexer) {
//                        float best = 0;
//                        //for each binary rule C -> C1 C2
//                        for (BinaryRule bR : grammar.getBinaryRulesByParent(labelIndexer.indexOf(C))) {
//                                float t1 = 0;
//                                float t2 = 0;
//                                float candidate = 0;
//                                if (unaryChart[min][mid][bR.getLeftChild()] != 0) {
//                                    t1 = unaryChart[min][mid][bR.getLeftChild()];
//                                    if (unaryChart[mid][max][bR.getRightChild()] != 0) {
//                                        t2 = unaryChart[mid][max][bR.getRightChild()];
//                                        double score = bR.getScore();
//                                        if (!Double.isNaN(score) && !Double.isInfinite(score)) {
//                                            double t = SloppyMath.logAdd(t1, t2);
//                                            candidate = (float) (SloppyMath.logAdd(t, bR.getScore()));
//                                            if (candidate > best) {
//                                                best = candidate;
//                                                binaryBackpointer = new int[]{bR.getLeftChild(), bR.getRightChild(), mid};
//                                            }
//                                        }
//                                    }
//                                }
//
//                        }
                    //                    System.out.println("In the binary chart from " + min + " to " + max + " for parent " + C + " the best children are " + labelIndexer.get(binaryBackpointer[0]) + " and " + labelIndexer.get(binaryBackpointer[1]) + " and the mid is " + binaryBackpointer[2]);
//                        binaryChart[min][max][labelIndexer.indexOf(C)] = best;
//                        binaryBackpointers[min][max][labelIndexer.indexOf(C)] = binaryBackpointer;
//                    }
                }
                float[] allC1s = binaryChart[min][max];
                for(short C1 = 1; C1 < allC1s.length; C1++) {
//                  for(String C : labelIndexer) {
                    if(allC1s[C1] != Float.NEGATIVE_INFINITY) {
                        short C = -1;
                        unaryBackpointer = new short[]{-1};
                        //for each unary rule C-> C1 after unary closure
                        for (UnaryRule uR : uc.getClosedUnaryRulesByChild(C1)) {
//                            if (binaryChart[min][max][C1] != Float.NEGATIVE_INFINITY) {
                                float t1 = binaryChart[min][max][C1];
                                double score = uR.getScore();
                                if (!Double.isNaN(score) && !Double.isInfinite(score)) {
                                    C = (short) uR.getParent();
                                    float candidate = (float)(t1 + score);
                                    if (candidate > unaryChart[min][max][C]) {
                                        unaryChart[min][max][C] = candidate;
                                        unaryBackpointer = new short[]{C1};
                                        unaryBackpointers[min][max][C] = unaryBackpointer;
                                    }
//                                }
                            }
                        }
                    }
                }
            }
        }

        short min=0;
        short max = size;
        String root = "ROOT^NULL";
        Tree<String> annotatedBestParse = new Tree<>(root);
        unaryBackpointer = unaryBackpointers[min][max][labelIndexer.indexOf(root)];
        Tree<String> child = subTreeFromUnaryRule(min, max, unaryBackpointer[0], sentenceArray, uc);
        annotatedBestParse.setChildren(Collections.singletonList(child));
        return MyTreeAnnotations.unAnnotateTree(annotatedBestParse);
    }

    private Tree<String> subTreeFromUnaryRule(short min, short max, short parent, String[] sentenceArray, UnaryClosure uc) {
        if(parent == 0) {
            System.err.println("The parent of a unary rule should never be 0 (ROOT^NULL).");
            exit(-1);
        }

        binaryBackpointer = binaryBackpointers[min][max][parent];
        short leftChild;
        short rightChild;
        short mid;
        if (binaryBackpointer.length == 3) {
            leftChild = binaryBackpointer[0];
            rightChild = binaryBackpointer[1];
            mid = binaryBackpointer[2];
        } else {
            return new Tree<>(labelIndexer.get(parent));
        }

//        List<Integer> leftChildPath = uc.getPath(new UnaryRule(parent, leftChild));
//        List<Integer> rightChildPath = uc.getPath(new UnaryRule(parent, rightChild));
//        List<String> rightChildLabelsPath = new ArrayList<>();
//        if(rightChildPath != null && !rightChildPath.isEmpty()) {
//            for (int i : rightChildPath) {
//                rightChildLabelsPath.add(labelIndexer.get(i));
//            }
//        }


        if((max - min) > 1) {
//            System.out.println("\nParent is " + labelIndexer.get(parent));
//            System.out.println("Left child is " + labelIndexer.get(leftChild));
//            System.out.println("Right child is " + labelIndexer.get(rightChild));
//            System.out.print("Right child path is ");
//            for(String label : rightChildLabelsPath) {
//                System.out.print(label + " -> ");
//            }
//            System.out.println();
//            System.out.println();
            return new Tree<>(labelIndexer.get(parent), Arrays.asList(subTreeFromBinaryRule(min, mid, leftChild, sentenceArray, uc), subTreeFromBinaryRule(mid, max, rightChild, sentenceArray, uc)));
        } else {
//            System.out.println("I think " + labelIndexer.get(parent) + " is a preterminal?\n");
            return new Tree<>(labelIndexer.get(parent), Collections.singletonList(new Tree<>(sentenceArray[min])));
        }
    }

    private Tree<String> subTreeFromBinaryRule(short min, short max, short parent, String[] sentenceArray, UnaryClosure uc) {
        unaryBackpointer = unaryBackpointers[min][max][parent]; //C1
        short child;
        if(unaryBackpointer.length==1) {
            child = unaryBackpointer[0];
        } else {
            return new Tree<>(labelIndexer.get(parent));
        }

        List<Integer> childPath = uc.getPath(new UnaryRule(parent, child));
        short size = 0;

        Tree<String> childPathChildren = new Tree<>("dummy");
        Tree<String> tempChildPathChildren = new Tree<>("dummy");
        if(childPath != null && !childPath.isEmpty()) {
            int[] childPathArray = childPath.stream().mapToInt(i->i).toArray();
            size = (short) childPathArray.length;


            childPathChildren = new Tree<>(labelIndexer.get(parent));
//            childPathChildren = new Tree<>(labelIndexer.get(childPath.get(0)));
            if(size>2) {
                tempChildPathChildren = unfoldPath(Arrays.copyOfRange(childPathArray, 1, (childPathArray.length-1)));
//                tempChildPathChildren = unfoldPath(childPath.subList(1, childPath.size()-1));
                int last = childPathArray[size-1];
//                int last = childPath.get(size-1);
                tempChildPathChildren.setChildren(Collections.singletonList(subTreeFromUnaryRule(min, max, (short) last, sentenceArray, uc)));
            }
        }


//        if((max - min) > 1) {
//        System.out.println("Parent is " + labelIndexer.get(parent));
//        System.out.println("Child is " + labelIndexer.get(child));
//        System.out.print("Child path is ");
//        for(String label : childLabelsPath) {
//            System.out.print(label + " -> ");
//        }
//        System.out.println();
//        System.out.println();


        //If it's not a reflexive rule and if there is a unary closure path
        if(child != parent) {
            if(size ==2){
                childPathChildren.setChildren(Collections.singletonList(subTreeFromUnaryRule(min, max, child, sentenceArray, uc)));
    //            childPathChildren.setChildren(Collections.singletonList(tempChildPathChildren));
                return childPathChildren;
            //return new Tree<>(labelIndexer.get(parent), Collections.singletonList(subTreeFromUnaryRule(min, max, child, sentence, uc)));
            } else if (size > 2) {
                childPathChildren.setChildren(Collections.singletonList(tempChildPathChildren));
                return childPathChildren;
            }
        }
//        else if (child == 0) {
////            System.out.println("I think " + labelIndexer.get(parent) + " is a preterminal?\n");
//            return new Tree<>(labelIndexer.get(parent), Collections.singletonList(new Tree<>(sentence.get(min))));
//        }
        else {
            return (subTreeFromUnaryRule(min, max, child, sentenceArray, uc));
        }
// } else {
// }           System.out.println("I think " + labelIndexer.get(parent) + " is a preterminal.\n");
//            return new Tree<>(labelIndexer.get(parent));
//        }
        return new Tree<>(labelIndexer.get(parent));
    }
    private Tree<String> unfoldPath(int[] childPathArray) {
        Tree<String> toReturn = new Tree<>(labelIndexer.get(childPathArray[0]));
        if (childPathArray.length == 1) {
            return toReturn;
        } else if (childPathArray.length ==2) {
            toReturn.setChildren(Collections.singletonList(new Tree<>(labelIndexer.get(childPathArray[1]))));
            return toReturn;
        }else {
            toReturn.setChildren(Collections.singletonList(unfoldPath(Arrays.copyOfRange(childPathArray, 1, childPathArray.length))));
//            toReturn.setChildren(Collections.singletonList(unfoldPath(childPath.subList(1, childPath.size()))));
            return toReturn;
        }
    }

    List<Tree<String>> annotateTrees(List<Tree<String>> trees) {
        List<Tree<String>> annotatedTrees = new ArrayList<>();
        for(Tree<String> tree : trees) {
            annotatedTrees.add(MyTreeAnnotations.annotateTree(tree));
        }
        return annotatedTrees;
    }
}
