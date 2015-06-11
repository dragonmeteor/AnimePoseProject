package yondoko.util;

import yondoko.util.EofException;
import yondoko.util.ParseException;

import java.io.IOException;
import java.io.Reader;
import java.util.LinkedList;

public class TextParser
{
    private Reader in;
    private LinkedList<Character> buffer;
    private boolean inEof = false;

    public TextParser(Reader in)
    {
        this.in = in;
        this.buffer = new LinkedList<Character>();
    }

    public void fetchChar() throws IOException
    {
        int result = in.read();
        if (result == -1)
        {
            inEof = true;
        }
        else
        {
            buffer.add((char) result);
        }
    }

    public char nextChar() throws IOException
    {
        if (buffer.size() == 0)
        {
            fetchChar();
        }
        if (buffer.isEmpty())
        {
            throw new EofException("attempt to load beyond the end of stream.");
        }
        char result = buffer.get(0);
        buffer.remove(0);
        return result;
    }

    public boolean isEof()
    {
        return buffer.isEmpty() && inEof;
    }

    public char peek() throws IOException
    {
        if (buffer.isEmpty())
        {
            fetchChar();
        }
        if (buffer.isEmpty())
        {
            throw new EofException("attemp to load beyond the end of stream");
        }
        return buffer.get(0);
    }

    public char peekNextChar() throws IOException
    {
        nextChar();
        return peek();
    }

    public void putBack(char c)
    {
        buffer.add(0, c);
    }

    public void consumeWhiteSpace() throws IOException
    {
        while (true)
        {
            try
            {
                char ch = peek();
                if (Character.isWhitespace(ch))
                {
                    nextChar();
                }
                else
                {
                    return;
                }
            }
            catch (EofException e)
            {
                return;
            }
        }
    }

    public int parseInt() throws IOException
    {
        consumeWhiteSpace();
        if (isEof())
        {
            throw new ParseException("int expected");
        }

        char ch = peek();
        if (!Character.isDigit(ch) && ch != '-')
        {
            throw new ParseException("int expected");
        }

        StringBuilder builder = new StringBuilder();
        try
        {
            if (ch == '-')
            {
                builder.append(ch);
                ch = peekNextChar();
            }
        }
        catch (EofException e)
        {
            throw new ParseException("int expected");
        }

        try
        {
            while (Character.isDigit(ch))
            {
                builder.append(ch);
                ch = peekNextChar();
            }
        }
        catch (EofException e)
        {
            // NOP
        }

        try
        {
            return Integer.valueOf(builder.toString());
        }
        catch (NumberFormatException e)
        {
            throw new ParseException("int expected");
        }
    }

    String parseFloatingPointString() throws IOException
    {
        consumeWhiteSpace();
        if (isEof())
        {
            throw new ParseException("float expected");
        }

        StringBuilder builder = new StringBuilder();
        char ch = peek();
        if (ch == '-' || ch == '+')
        {
            builder.append(ch);
            ch = peekNextChar();
        }

        try
        {
            boolean foundDot = false;
            while (Character.isDigit(ch) || ch == '.')
            {
                if (!foundDot && ch == '.')
                {
                    foundDot = true;
                }
                else if (foundDot && ch == '.')
                {
                    return builder.toString();
                }

                builder.append(ch);
                ch = peekNextChar();
            }
        }
        catch (EofException e)
        {
            return builder.toString();
        }

        if (ch == 'e' || ch == 'E')
        {
            builder.append(ch);
            ch = peekNextChar();

            boolean foundSign = false;
            if (ch == '-' || ch == '+')
            {
                builder.append(ch);
                ch = peekNextChar();
                foundSign = true;
            }

            int digitCount = 0;
            try
            {
                while (Character.isDigit(ch))
                {
                    builder.append(ch);
                    ch = peekNextChar();
                    digitCount++;
                }
            }
            catch (EofException e)
            {
                // NOP
            }

            if (digitCount <= 0)
            {
                if (foundSign)
                {
                    char sign = builder.charAt(builder.length() - 1);
                    putBack(sign);
                    builder.deleteCharAt(builder.length() - 1);
                }

                char exp = builder.charAt(builder.length() - 1);
                putBack(exp);
                builder.deleteCharAt(builder.length() - 1);
            }
        }

        return builder.toString();
    }

    public float parseFloat() throws IOException
    {
        try
        {
            return Float.valueOf(parseFloatingPointString());
        }
        catch (NumberFormatException e)
        {
            throw new ParseException("float expected");
        }
    }

    public double parseDouble() throws IOException
    {
        try
        {
            return Double.valueOf(parseFloatingPointString());
        }
        catch (NumberFormatException e)
        {
            throw new ParseException("float expected");
        }
    }

    public String parseContiguousString() throws IOException
    {
        consumeWhiteSpace();
        if (isEof())
        {
            throw new ParseException("string expected");
        }

        StringBuilder builder = new StringBuilder();
        try
        {
            char ch = peek();
            while (!Character.isWhitespace(ch))
            {
                builder.append(ch);
                ch = peekNextChar();
            }
        }
        catch (EofException e)
        {
            // NOP
        }
        return builder.toString();
    }

    public String parseIdentifier() throws IOException
    {
        consumeWhiteSpace();
        if (isEof())
        {
            throw new ParseException("identifier expected");
        }

        char ch = peek();
        if (!Character.isLetter(ch) && ch != '_')
        {
            throw new ParseException("identifier expected");
        }

        StringBuilder builder = new StringBuilder();
        try
        {
            while (Character.isLetterOrDigit(ch) || ch == '_')
            {
                builder.append(ch);
                ch = peekNextChar();
            }
        }
        catch (EofException e)
        {
            // NOP
        }
        return builder.toString();
    }

    public void matchString(String s) throws IOException
    {
        try
        {
            for (int i = 0; i < s.length(); i++)
            {
                char ch = nextChar();
                if (ch != s.charAt(i))
                {
                    throw new ParseException("'" + s + "' expected");
                }
            }
        }
        catch (EofException e)
        {
            throw new ParseException("'" + s + "' expected by reached EOF");
        }
    }

    public void matchStringCaseInsensitive(String s) throws IOException
    {
        try
        {
            for (int i = 0; i < s.length(); i++)
            {
                char actual = nextChar();
                char expected = Character.toUpperCase(s.charAt(i));
                if (Character.isLetter(expected))
                {
                    if (Character.toUpperCase(actual) != expected)
                    {
                        throw new ParseException("'" + s + "' (case insensitive expected ");
                    }
                }
                else if (actual != expected)
                {
                    throw new ParseException("'" + s + "' (case insensitive expected ");
                }
            }
        }
        catch (EofException e)
        {
            throw new ParseException("'" + s + "' (case insensitive expected "
                    + "but reached EOF");
        }
    }

    public String parseQuotedString() throws IOException
    {
        consumeWhiteSpace();
        try
        {
            matchString("\"");
            StringBuilder builder = new StringBuilder();
            char ch = peek();
            while (ch != '"')
            {
                builder.append(ch);
                ch = peekNextChar();
            }
            matchString("\"");
            return builder.toString();
        }
        catch (ParseException e)
        {
            throw new ParseException("quoted string expected");
        }
        catch (EofException e)
        {
            throw new ParseException("quoted string expected but reached EOF");
        }
    }

    public String parseFixedLengthString(int length) throws IOException
    {
        try
        {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < length; i++)
            {
                char ch = nextChar();
                builder.append(ch);
            }
            return builder.toString();
        }
        catch (EofException e)
        {
            throw new ParseException("a string of length at least " + length
                    + " is required");
        }
    }       

    public void putBack(String s)
    {
        for (int i = s.length()-1; i >= 0; i--)
        {
            putBack(s.charAt(i));
        }
    }
}
