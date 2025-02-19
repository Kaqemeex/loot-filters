package com.lootfilters.lang;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import static com.lootfilters.util.TextUtil.isLegalIdent;
import static com.lootfilters.util.TextUtil.isNumeric;
import static com.lootfilters.util.TextUtil.isWhitespace;

@RequiredArgsConstructor
public class Lexer {
    private static final LinkedHashMap<String, Token.Type> STATICS = new LinkedHashMap<>() {{
        put("\\\n", Token.Type.WHITESPACE);
        put("#define", Token.Type.PREPROC_DEFINE);
        put("apply", Token.Type.APPLY);
        put("false", Token.Type.FALSE);
        put("true", Token.Type.TRUE);
        put("meta", Token.Type.META);
        put("if", Token.Type.IF);
        put("&&", Token.Type.OP_AND);
        put("||", Token.Type.OP_OR);
        put(">=", Token.Type.OP_GTEQ);
        put("<=", Token.Type.OP_LTEQ);
        put("==", Token.Type.OP_EQ);
        put("!", Token.Type.OP_NOT);
        put(">", Token.Type.OP_GT);
        put("<", Token.Type.OP_LT);
        put(";", Token.Type.STMT_END);
        put(":", Token.Type.COLON);
        put("=", Token.Type.ASSIGN);
        put(",", Token.Type.COMMA);
        put("(", Token.Type.EXPR_START);
        put(")", Token.Type.EXPR_END);
        put("{", Token.Type.BLOCK_START);
        put("}", Token.Type.BLOCK_END);
        put("[", Token.Type.LIST_START);
        put("]", Token.Type.LIST_END);
        put("\n", Token.Type.NEWLINE);
        put("\r", Token.Type.NEWLINE);
    }};

    private final String input;
    private final List<Token> tokens = new ArrayList<>();
    private int inputOffset = 0;
    private int currentLineOffset = 0;
    // 1 indexed cause all editors start line counts at 1
    private int currentLineNumber = 1;

    public List<Token> tokenize() throws TokenizeException {
        while (inputOffset < input.length()) {
            if (tokenizeStatic()) {
                continue;
            }
            if (tokenizeComment()) {
                continue;
            }

            var ch = input.charAt(inputOffset);
            if (isWhitespace(ch)) {
                // TODO - This should not actually process newlines
                // If it does then that messes up our line counter
                tokenizeWhitespace();
            } else if (isNumeric(ch)) {
                tokenizeLiteralInt();
            } else if (ch == '"') {
                tokenizeLiteralString();
            } else if (isLegalIdent(ch)) {
                tokenizeIdentifier();
            } else {
                throw new TokenizeException(String.format("unrecognized character '" + ch + "' line %s char %s", currentLineNumber, currentLineOffset));
            }
        }

        return tokens.stream() // un-map escaped newlines
                .map(it -> it.getValue().equals("\\\n") ? new Token(Token.Type.WHITESPACE, "", it.getLocation()) : it)
                .collect(Collectors.toList());
    }

    private boolean tokenizeStatic() {
        for (var entry : STATICS.entrySet()) {
            var value = entry.getKey();
            var type = entry.getValue();
            if (input.startsWith(value, inputOffset)) {
                tokens.add(new Token(type, value, currentLocation()));
                inputOffset += value.length();
                if (type == Token.Type.NEWLINE) {
                    currentLineOffset = 0;
                    currentLineNumber += 1;
                } else {
                    currentLineOffset += value.length();
                }
                return true;
            }
        }
        return false;
    }

    private boolean tokenizeComment() {
        if (!input.startsWith("//", inputOffset)) {
            return false;
        }

        var lineEnd = input.indexOf('\n', inputOffset);
        var text = lineEnd > -1
                ? input.substring(inputOffset, lineEnd)
                : input.substring(inputOffset);
        tokens.add(new Token(Token.Type.COMMENT, text, currentLocation()));
        inputOffset += text.length();
        return true;
    }

    private void tokenizeWhitespace() {
        for (int i = inputOffset; i < input.length(); ++i) {
            if (!isWhitespace(input.charAt(i))) {
                var ws = input.substring(inputOffset, i);
                tokens.add(new Token(Token.Type.WHITESPACE, ws, currentLocation()));
                inputOffset += i - inputOffset;
                return;
            }
        }
        tokens.add(new Token(Token.Type.WHITESPACE, input.substring(inputOffset), currentLocation()));
        inputOffset = input.length();
    }

    private void tokenizeLiteralInt() {
        for (int i = inputOffset; i < input.length(); ++i) {
            if (input.charAt(i) == '_') {
                continue;
            }
            if (!isNumeric(input.charAt(i))) {
                var literal = input.substring(inputOffset, i);
                tokens.add(Token.intLiteral(literal, currentLocation()));
                inputOffset += literal.length();
                return;
            }
        }
        tokens.add(Token.intLiteral(input.substring(inputOffset), currentLocation()));
        inputOffset = input.length();
    }

    private void tokenizeLiteralString() throws TokenizeException {
        for (int i = inputOffset +1; i < input.length(); ++i) {
            if (input.charAt(i) == '"') {
                var literal = input.substring(inputOffset +1, i);
                tokens.add(Token.stringLiteral(literal, currentLocation()));
                inputOffset += literal.length() + 2; // for quotes, which the captured literal omits
                return;
            }
        }
        throw new TokenizeException("unterminated string literal");
    }

    private void tokenizeIdentifier() {
        for (int i = inputOffset; i < input.length(); ++i) {
            if (!isLegalIdent(input.charAt(i))) {
                var ident = input.substring(inputOffset, i);
                tokens.add(Token.identifier(ident, currentLocation()));
                inputOffset += ident.length();
                return;
            }
        }
        tokens.add(Token.identifier(input.substring(inputOffset), currentLocation()));
        inputOffset = input.length();
    }

    private Location currentLocation() {
        return new Location(currentLineNumber, currentLineOffset);
    }
}
