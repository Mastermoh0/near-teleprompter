package com.mastermoh.near;

import org.junit.Test;
import static org.junit.Assert.*;

public class ScriptMatcherTest {
    @Test public void followsGrowingPartialResultsWithoutRepeatingProgress() {
        ScriptMatcher match=new ScriptMatcher("Hello everyone today we have something to share");
        match.accept("hello everyone"); assertEquals(2,match.wordPosition());
        match.accept("hello everyone today"); assertEquals(3,match.wordPosition());
        match.accept("hello everyone today"); assertEquals(3,match.wordPosition());
    }
    @Test public void silenceAndUnrelatedSpeechHoldPosition() {
        ScriptMatcher match=new ScriptMatcher("Welcome to this video about making your next take");
        match.accept("welcome to this video"); int position=match.character();
        match.accept(""); match.accept("purple elephants dancing quietly"); assertEquals(position,match.character());
    }
    @Test public void punctuationCaseAndApostrophesAreNormalized() {
        ScriptMatcher match=new ScriptMatcher("Hello, WORLD! Don't worry about it.");
        match.accept("hello world dont worry"); assertEquals(4,match.wordPosition());
    }
    @Test public void oneRecognitionMistakeCanRecoverWithContext() {
        ScriptMatcher match=new ScriptMatcher("Today we will build a camera inside our application");
        match.accept("today we will built a camera"); assertEquals(6,match.wordPosition());
    }
    @Test public void omittedWordCanRecover() {
        ScriptMatcher match=new ScriptMatcher("We can now record a clear video inside Near");
        match.accept("we can record a clear video"); assertEquals(7,match.wordPosition());
    }
    @Test public void repetitionCanReturnToAnEarlierSentence() {
        ScriptMatcher match=new ScriptMatcher("This is my first sentence and here is my second sentence today");
        match.accept("this is my first sentence and here is"); assertEquals(8,match.wordPosition());
        match.accept("this is my first"); assertEquals(4,match.wordPosition());
    }
    @Test public void repeatedPhraseDoesNotJumpToDistantCopy() {
        ScriptMatcher match=new ScriptMatcher("Here is the plan for today and here is the plan for tomorrow");
        match.accept("here is the plan"); assertEquals(4,match.wordPosition());
        match.accept("here is the plan"); assertEquals(4,match.wordPosition());
    }
    @Test public void manualSeekingChangesTheSearchAnchor() {
        String script="An opening sentence. A middle sentence. A closing sentence.";
        ScriptMatcher match=new ScriptMatcher(script); match.seekCharacter(script.indexOf("closing"));
        match.accept("a closing sentence"); assertEquals(script.length(),match.character());
        match.seekCharacter(0); assertEquals(0,match.wordPosition());
    }
    @Test public void aSingleCommonWordCannotJumpAcrossTheScript() {
        ScriptMatcher match=new ScriptMatcher("Start with a clear thought then we explain the next part");
        match.accept("the"); assertEquals(0,match.wordPosition());
    }
    @Test public void finalWordAndEmptyScriptsAreBounded() {
        ScriptMatcher match=new ScriptMatcher("one clear thought");
        match.accept("one clear thought"); assertEquals(17,match.character());
        ScriptMatcher empty=new ScriptMatcher(""); empty.accept("hello world"); assertEquals(0,empty.character());
    }
    @Test public void twoCommonWordsCannotSkipAnEntireParagraph() {
        ScriptMatcher match=new ScriptMatcher("Start with one clear thought about your story before we move on and then show the result");
        match.accept("and then"); assertEquals(0,match.wordPosition());
    }
}
