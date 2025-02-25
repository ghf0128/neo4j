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
package org.neo4j.server.startup;

public class Neo4jBoot
{

    /**
     * IMPORTANT NOTE! This class is compiled using Java 7 and can not use any dependencies or include any other class. Its only purpose is to print a useful
     * error message when Neo4j (bootloader) is started using an old, unsupported java.
     */
    public static void main( String[] args )
    {
        printJavaVersionErrorMessage();
    }

    static void printJavaVersionErrorMessage()
    {
        String version = System.getProperty( "java.version" );
        System.out.println( "Unsupported Java " + version
                + " detected. Please use Oracle(R) Java(TM) 11, OpenJDK(TM) 11 to run Neo4j Server." );
    }
}
