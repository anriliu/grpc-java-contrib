/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.jprotoc;

import com.google.common.base.CaseFormat;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.DescriptorProtos;

import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * {@code ProtoTypeMap} maintains a dictionary for looking up Java type names when given proto types.
 */
public final class ProtoTypeMap {

    private final ImmutableMap<String, String> types;

    private ProtoTypeMap(@Nonnull ImmutableMap<String, String> types) {
        Preconditions.checkNotNull(types, "types");

        this.types = types;
    }

    /**
     * Returns an instance of {@link ProtoTypeMap} based on the given FileDescriptorProto instances.
     *
     * @param fileDescriptorProtos the full collection of files descriptors from the code generator request
     */
    public static ProtoTypeMap of(@Nonnull Collection<DescriptorProtos.FileDescriptorProto> fileDescriptorProtos) {
        Preconditions.checkNotNull(fileDescriptorProtos, "fileDescriptorProtos");
        Preconditions.checkArgument(!fileDescriptorProtos.isEmpty(), "fileDescriptorProtos.isEmpty()");

        final ImmutableMap.Builder<String, String> types = ImmutableMap.builder();

        for (final DescriptorProtos.FileDescriptorProto fileDescriptor : fileDescriptorProtos) {
            final DescriptorProtos.FileOptions fileOptions = fileDescriptor.getOptions();

            final String protoPackage = fileDescriptor.hasPackage() ? "." + fileDescriptor.getPackage() : "";
            final String javaPackage = Strings.emptyToNull(
                    fileOptions.hasJavaPackage() ?
                            fileOptions.getJavaPackage() :
                            fileDescriptor.getPackage());
            final String enclosingClassName =
                    fileOptions.getJavaMultipleFiles() ?
                            null :
                            getJavaOuterClassname(fileDescriptor, fileOptions);

            fileDescriptor.getEnumTypeList().forEach(
                e -> types.put(
                        protoPackage + "." + e.getName(),
                        toJavaTypeName(e.getName(), enclosingClassName, javaPackage)));

            fileDescriptor.getMessageTypeList().forEach(
                m -> types.put(
                        protoPackage + "." + m.getName(),
                        toJavaTypeName(m.getName(), enclosingClassName, javaPackage)));
        }

        return new ProtoTypeMap(types.build());
    }

    /**
     * Returns the full Java type name for the given proto type.
     *
     * @param protoTypeName the proto type to be converted to a Java type
     */
    public String toJavaTypeName(@Nonnull String protoTypeName) {
        Preconditions.checkNotNull(protoTypeName, "protoTypeName");
        return types.get(protoTypeName);
    }

    /**
     * Returns the full Java type name based on the given protobuf type parameters.
     *
     * @param className the protobuf type name
     * @param enclosingClassName the optional enclosing class for the given type
     * @param javaPackage the proto file's configured java package name
     */
    public static String toJavaTypeName(
            @Nonnull String className,
            @Nullable String enclosingClassName,
            @Nullable String javaPackage) {

        Preconditions.checkNotNull(className, "className");

        Joiner dotJoiner = Joiner.on('.').skipNulls();
        return dotJoiner.join(javaPackage, enclosingClassName, className);
    }

    private static String getJavaOuterClassname(
            DescriptorProtos.FileDescriptorProto fileDescriptor,
            DescriptorProtos.FileOptions fileOptions) {

        if (fileOptions.hasJavaOuterClassname()) {
            return fileOptions.getJavaOuterClassname();
        }

        // If the outer class name is not explicitly defined, then we take the proto filename, strip its extension,
        // and convert it from snake case to camel case.
        String filename = fileDescriptor.getName().substring(0, fileDescriptor.getName().length() - ".proto".length());

        return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, filename);
    }
}
