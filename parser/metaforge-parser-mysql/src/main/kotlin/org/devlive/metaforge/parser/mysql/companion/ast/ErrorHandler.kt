/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.devlive.metaforge.parser.mysql.companion.ast

import com.google.common.base.MoreObjects
import com.google.common.collect.ImmutableSet
import org.devlive.metaforge.parser.mysql.companion.ParserException
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.atn.*
import org.antlr.v4.runtime.misc.IntervalSet
import java.util.*
import java.util.stream.Collectors

class ErrorHandler
private constructor(specialRules: Map<Int, String>, private val specialTokens: Map<Int, String>, ignoredRules: Set<Int>) : BaseErrorListener()
{
    private val specialRules: Map<Int, String> = HashMap(specialRules)
    private val ignoredRules: Set<Int> = HashSet(ignoredRules)

    override fun syntaxError(recognizer: Recognizer<*, *>, offendingSymbol: Any, line: Int, charPositionInLine: Int, message: String, e: RecognitionException)
    {
        var message: String? = message
        try
        {
            val parser = recognizer as Parser

            val atn = parser.atn

            val currentState: ATNState
            var currentToken: Token
            val context: RuleContext

            if (e != null)
            {
                currentState = atn.states[e.offendingState]
                currentToken = e.offendingToken
                context = e.ctx

                if (e is NoViableAltException)
                {
                    currentToken = e.startToken
                }
            }
            else
            {
                currentState = atn.states[parser.state]
                currentToken = parser.currentToken
                context = parser.context
            }

            val analyzer = Analyzer(parser, specialRules, specialTokens, ignoredRules)
            val result = analyzer.process(currentState, currentToken.tokenIndex, context)

            // pick the candidate tokens associated largest token index processed (i.e., the path that consumed the most input)
            val expected: String = result.expected.stream()
                .sorted()
                .collect(Collectors.joining(", "))

            message = String.format("mismatched input '%s'. Expecting: %s", parser.tokenStream[result.errorTokenIndex].text, expected)
        }
        catch (exception: Exception)
        {
            //            log.error(exception, "Unexpected failure when handling parsing error. This is likely a bug in the implementation");
        }
        throw ParserException(message !!, e, line, charPositionInLine)
    }

    private class ParsingState
        (val state: ATNState, val tokenIndex: Int, val suppressed: Boolean, val parser: Parser)
    {
        override fun equals(o: Any?): Boolean
        {
            if (this === o)
            {
                return true
            }
            if (o == null || javaClass != o.javaClass)
            {
                return false
            }
            val that = o as ParsingState
            return tokenIndex == that.tokenIndex && state == that.state
        }

        override fun hashCode(): Int
        {
            return Objects.hash(state, tokenIndex)
        }

        override fun toString(): String
        {
            val token = parser.tokenStream[tokenIndex]

            var text = MoreObjects.firstNonNull(token.text, "?")
            text = text.replace("\\", "\\\\")
            text = text.replace("\n", "\\n")
            text = text.replace("\r", "\\r")
            text = text.replace("\t", "\\t")

            return String.format(
                "%s%s:%s @ %s:<%s>:%s",
                if (suppressed) "-" else "+",
                parser.ruleNames[state.ruleIndex],
                state.stateNumber,
                tokenIndex,
                parser.vocabulary.getSymbolicName(token.type),
                text
            )
        }
    }

    private class Analyzer
        (
        private val parser: Parser,
        private val specialRules: Map<Int, String>,
        private val specialTokens: Map<Int, String>,
        private val ignoredRules: Set<Int>
    )
    {
        private val atn: ATN = parser.atn
        private val vocabulary: Vocabulary = parser.vocabulary
        private val stream: TokenStream = parser.tokenStream
        private val candidates: MutableSet<String?> = HashSet()
        private val memo: MutableMap<ParsingState, Set<Int>?> = HashMap()
        private var furthestTokenIndex = - 1

        fun process(currentState: ATNState, tokenIndex: Int, context: RuleContext): Result
        {
            var currentState = currentState
            var context = context
            val startState = atn.ruleToStartState[currentState.ruleIndex]

            if (isReachable(currentState, startState))
            {
                // We've been dropped inside a rule in a state that's reachable via epsilon transitions. This is,
                // effectively, equivalent to starting at the beginning (or immediately outside) the rule.
                // In that case, backtrack to the beginning to be able to take advantage of logic that remaps
                // some rules to well-known names for reporting purposes
                currentState = startState
            }

            var endTokens = process(ParsingState(currentState, tokenIndex, false, parser), 0)
            val nextTokens: MutableSet<Int> = HashSet()
            while (endTokens !!.isNotEmpty() && context.invokingState != - 1)
            {
                for (endToken in endTokens)
                {
                    val nextState = (atn.states[context.invokingState].transition(0) as RuleTransition).followState
                    nextTokens.addAll(process(ParsingState(nextState, endToken, false, parser), 0) !!)
                }
                context = context.parent
                endTokens = nextTokens
            }

            return Result(furthestTokenIndex, candidates)
        }

        private fun isReachable(target: ATNState, from: RuleStartState): Boolean
        {
            val activeStates: Deque<ATNState> = ArrayDeque()
            activeStates.add(from)

            while (! activeStates.isEmpty())
            {
                val current = activeStates.pop()

                if (current.stateNumber == target.stateNumber)
                {
                    return true
                }

                for (i in 0 until current.numberOfTransitions)
                {
                    val transition = current.transition(i)

                    if (transition.isEpsilon)
                    {
                        activeStates.push(transition.target)
                    }
                }
            }

            return false
        }

        private fun process(start: ParsingState, precedence: Int): Set<Int>?
        {
            var result = memo[start]
            if (result != null)
            {
                return result
            }

            val endTokens = ImmutableSet.builder<Int>()

            // Simulates the ATN by consuming input tokens and walking transitions.
            // The ATN can be in multiple states (similar to an NFA)
            val activeStates: Deque<ParsingState> = ArrayDeque()
            activeStates.add(start)

            while (! activeStates.isEmpty())
            {
                val current = activeStates.pop()

                val state = current.state
                var tokenIndex = current.tokenIndex
                var suppressed = current.suppressed

                while (stream[tokenIndex].channel == Token.HIDDEN_CHANNEL)
                {
                    // Ignore whitespace
                    tokenIndex ++
                }
                val currentToken = stream[tokenIndex].type

                if (state.stateType == ATNState.RULE_START)
                {
                    val rule = state.ruleIndex

                    if (specialRules.containsKey(rule))
                    {
                        if (! suppressed)
                        {
                            record(tokenIndex, specialRules[rule])
                        }
                        suppressed = true
                    }
                    else if (ignoredRules.contains(rule))
                    {
                        // TODO expand ignored rules like we expand special rules
                        continue
                    }
                }

                if (state is RuleStopState)
                {
                    endTokens.add(tokenIndex)
                    continue
                }

                for (i in 0 until state.numberOfTransitions)
                {
                    val transition = state.transition(i)

                    if (transition is RuleTransition)
                    {
                        for (endToken in process(ParsingState(transition.target, tokenIndex, suppressed, parser), transition.precedence) !!)
                        {
                            activeStates.push(ParsingState(transition.followState, endToken, suppressed, parser))
                        }
                    }
                    else if (transition is PrecedencePredicateTransition)
                    {
                        if (precedence < transition.precedence)
                        {
                            activeStates.push(ParsingState(transition.target, tokenIndex, suppressed, parser))
                        }
                    }
                    else if (transition.isEpsilon)
                    {
                        activeStates.push(ParsingState(transition.target, tokenIndex, suppressed, parser))
                    }
                    else if (transition is WildcardTransition)
                    {
                        throw UnsupportedOperationException("not yet implemented: wildcard transition")
                    }
                    else
                    {
                        var labels = transition.label()

                        if (transition is NotSetTransition)
                        {
                            labels = labels.complement(IntervalSet.of(Token.MIN_USER_TOKEN_TYPE, atn.maxTokenType))
                        }

                        // Surprisingly, TokenStream (i.e. BufferedTokenStream) may not have loaded all the tokens from the
                        // underlying stream. TokenStream.get() does not force tokens to be buffered -- it just returns what's
                        // in the current buffer, or fail with an IndexOutOfBoundsError. Since Antlr decided the error occurred
                        // within the current set of buffered tokens, stop when we reach the end of the buffer.
                        if (labels.contains(currentToken) && tokenIndex < stream.size() - 1)
                        {
                            activeStates.push(ParsingState(transition.target, tokenIndex + 1, false, parser))
                        }
                        else
                        {
                            if (! suppressed)
                            {
                                record(tokenIndex, getTokenNames(labels))
                            }
                        }
                    }
                }
            }

            result = endTokens.build()
            memo[start] = result
            return result
        }

        private fun record(tokenIndex: Int, label: String?)
        {
            record(tokenIndex, ImmutableSet.of(label))
        }

        private fun record(tokenIndex: Int, labels: Set<String?>)
        {
            if (tokenIndex >= furthestTokenIndex)
            {
                if (tokenIndex > furthestTokenIndex)
                {
                    candidates.clear()
                    furthestTokenIndex = tokenIndex
                }

                candidates.addAll(labels)
            }
        }

        private fun getTokenNames(tokens: IntervalSet): Set<String?>
        {
            val names: MutableSet<String?> = HashSet()
            for (i in 0 until tokens.size())
            {
                val token = tokens[i]
                if (token == Token.EOF)
                {
                    names.add("<EOF>")
                }
                else
                {
                    names.add(specialTokens.getOrDefault(token, vocabulary.getDisplayName(token)))
                }
            }

            return names
        }
    }

    class Builder
    {
        private val specialRules: MutableMap<Int, String> = HashMap()
        private val specialTokens: MutableMap<Int, String> = HashMap()
        private val ignoredRules: MutableSet<Int> = HashSet()

        fun specialRule(ruleId: Int, name: String): Builder
        {
            specialRules[ruleId] = name
            return this
        }

        fun specialToken(tokenId: Int, name: String): Builder
        {
            specialTokens[tokenId] = name
            return this
        }

        fun ignoredRule(ruleId: Int): Builder
        {
            ignoredRules.add(ruleId)
            return this
        }

        fun build(): ErrorHandler
        {
            return ErrorHandler(specialRules, specialTokens, ignoredRules)
        }
    }

    private class Result
        (val errorTokenIndex: Int, val expected: Set<String?>)

    companion object
    {
        fun builder(): Builder
        {
            return Builder()
        }
    }
}
