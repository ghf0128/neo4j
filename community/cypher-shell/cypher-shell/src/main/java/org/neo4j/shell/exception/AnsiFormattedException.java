/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.shell.exception;


import org.neo4j.shell.log.AnsiFormattedText;

/**
 * A type of exception where the message can formatted with Ansi codes.
 */
public class AnsiFormattedException extends Exception
{
    private final AnsiFormattedText message;

    public AnsiFormattedException( String message )
    {
        super( message );
        this.message = AnsiFormattedText.from( message );
    }

    public AnsiFormattedException( String message, Throwable cause )
    {
        super( message, cause );
        this.message = AnsiFormattedText.from( message );
    }

    public AnsiFormattedException( AnsiFormattedText message )
    {
        super( message.plainString() );
        this.message = message;
    }

    public AnsiFormattedText getFormattedMessage()
    {
        return message;
    }
}
