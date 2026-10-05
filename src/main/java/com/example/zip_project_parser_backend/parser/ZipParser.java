package com.example.zip_project_parser_backend.parser;

import com.example.zip_project_parser_backend.dto.*;
import com.example.zip_project_parser_backend.enumeration.ClassKind;
import com.example.zip_project_parser_backend.enumeration.HttpMethodKind;
import com.example.zip_project_parser_backend.enumeration.InheritanceDependencyKind;
import com.example.zip_project_parser_backend.enumeration.MethodDependencyKind;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class ZipParser {

    public Map<String, String> uploadAndParseZip(MultipartFile zip) {

        Map<String, String> classesAndCode = new HashMap<>();

        try(var inputStream = new ZipInputStream(zip.getInputStream()); ){
            ZipEntry entry;
            while ((entry = inputStream.getNextEntry()) != null) {
                if (entry.getName().startsWith("_") || !entry.getName().endsWith(".java")) {
                    continue;
                }
                StringBuilder code = new StringBuilder();

                Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8);
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    code.append(line).append("\n");
                }

                classesAndCode.put(entry.getName(), code.toString());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return classesAndCode;
    }

    public ProjectAnalysisResponse getClassDetails(Map<String, String> map) {

        Map<String, List<ClassDetailsResponse>> returnMap = new HashMap<>();
        Map<String, List<ImportResponse>> importsByPath = new HashMap<>();
        Map<String, List<ImportResponse>> importsByQualifiedName = new HashMap<>();
        Map<String, List<CallSite>> methodCalls = new HashMap<>();
        List<EndpointResponse> endpointResponseList = new ArrayList<>();
        List<MethodCallResponse> methodCallResponseList = new ArrayList<>();
        var path = "";
        List<AnalysisWarning> warnings = new ArrayList<>();
        for (var entry : map.entrySet()) {
            List<ClassDetailsResponse> classDetailsResponses = new ArrayList<>();
            List<CallSite> methodCallsResponses = new ArrayList<>();
            var classCode = entry.getValue();
            path = entry.getKey();

            JavaParser javaParser = new JavaParser();
            javaParser.getParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_25);
            var parseResult = javaParser.parse(classCode);
            if(!parseResult.isSuccessful()) {
                warnings.add(new AnalysisWarning(path ,parseResult.getProblems().getFirst().getVerboseMessage()));
                continue;
            }

            CompilationUnit parsedCode = parseResult.getResult().orElseThrow();
            var packageName = "";
            var packageDeclaration = parsedCode.getPackageDeclaration().orElse(null);
            if (packageDeclaration != null) {
                packageName = packageDeclaration.getNameAsString();
            }

            var imports = extractImports(parsedCode);
            importsByPath.put(path, imports);

            for (var i = 0; i < parsedCode.getTypes().size(); i++) {
                var type = parsedCode.getType(i);

                var className = type.getNameAsString();
                var annotationsLoop = type.getAnnotations()
                        .stream()
                        .map(Node::toString)
                        .toList();

                ClassKind classKind = null;
                List<String> constants = new ArrayList<>();

                if (type.isRecordDeclaration()) {
                    classKind = ClassKind.RECORD;
                } else if (type.isAnnotationDeclaration()) {
                    classKind = ClassKind.ANNOTATION;
                } else if (type.isEnumDeclaration()) {
                    classKind = ClassKind.ENUM;
                    var enumDeclaration = type.asEnumDeclaration();
                    for (var k = 0; k < enumDeclaration.getEntries().size(); k++) {
                        var constant = enumDeclaration.getEntry(k).getNameAsString();
                        constants.add(constant);
                    }
                } else if (type.isClassOrInterfaceDeclaration()){
                    if (type.asClassOrInterfaceDeclaration().isInterface()) {
                        classKind = ClassKind.INTERFACE;
                    } else {
                        classKind = ClassKind.CLASS;
                    }
                }

                List<MethodResponse> methodResponses = new ArrayList<>(extractMethods(type));
                List<FieldResponse> fieldResponses = new ArrayList<>(extractFields(type));
                List<ConstructorResponse> constructorResponses = new ArrayList<>(extractConstructors(type));

                InheritanceResponse inheritanceResponses = extractInheritance(type);
                ClassDetailsResponse cnmr = new ClassDetailsResponse(packageName, methodResponses,
                        fieldResponses, constructorResponses, inheritanceResponses,
                        new ClassNameResponse(className, classKind, constants, annotationsLoop));
                classDetailsResponses.add(cnmr);
                String declaringTypeName;
                if (packageName == null || packageName.isEmpty()) {
                    declaringTypeName = className;
                } else {
                    declaringTypeName = packageName + "." + className;
                }
                endpointResponseList.addAll(extractEndpoints(type, declaringTypeName));
                methodCallsResponses.addAll(extractMethodCalls(type, declaringTypeName));
                importsByQualifiedName.put(declaringTypeName, imports);
            }
            methodCalls.put(path, methodCallsResponses);
            returnMap.put(path, classDetailsResponses);
        }
        var typesByQualifiedName = mapTypesByQualifiedName(returnMap);
        List<FieldDependencyResponse> fieldDependencyResponseList = new ArrayList<>();
        List<MethodDependencyResponse> methodDependencyResponseList = new ArrayList<>();
        List<ConstructorDependencyResponse> constructorDependencyResponseList = new ArrayList<>();
        List<InheritanceDependencyResponse> inheritanceDependencyResponseList = new ArrayList<>();

        for (var returnedMap : returnMap.entrySet()) {
            var key = returnedMap.getKey();
            var imports = importsByPath.get(key);
            var sourceTypes = returnedMap.getValue();
            var calls = methodCalls.get(key);
            IdentityHashMap<MethodCallExpr, CallSite> sitesByExpr = new IdentityHashMap<>();
            IdentityHashMap<MethodCallExpr, MethodCallResponse> resolvedByExpr = new IdentityHashMap<>();

            for (var call : calls) {
                sitesByExpr.put(call.methodCallExpr(), call);
            }

            for (var sourceType : sourceTypes) {
                String sourceTypeName;
                if (sourceType.packageName() == null || sourceType.packageName().isEmpty()) {
                    sourceTypeName = sourceType.classNameResponse().className();
                } else {
                    sourceTypeName = sourceType.packageName() + "." + sourceType.classNameResponse().className();
                }
                var resolvedFieldDependencies = resolveFieldDependencies(sourceType, imports, typesByQualifiedName, sourceTypeName);
                fieldDependencyResponseList.addAll(resolvedFieldDependencies);

                var resolvedMethodDependencies = resolveMethodDependencies(sourceType, imports, typesByQualifiedName, sourceTypeName);
                methodDependencyResponseList.addAll(resolvedMethodDependencies);

                var resolvedConstructorDependencies = resolveConstructorDependencies(sourceType, imports, typesByQualifiedName, sourceTypeName);
                constructorDependencyResponseList.addAll(resolvedConstructorDependencies);

                var resolvedInheritanceDependencies = resolveInheritanceDependencies(sourceType, imports, typesByQualifiedName, sourceTypeName);
                inheritanceDependencyResponseList.addAll(resolvedInheritanceDependencies);

                for (var call : calls) {
                    var callRsp = call.methodCallResponse();
                    if (callRsp.sourceType().equals(sourceTypeName)) {
                        methodCallResponseList.add(
                                resolveCall(call, sourceTypeName, sourceType.packageName(),
                                        imports, typesByQualifiedName, sitesByExpr, resolvedByExpr, importsByQualifiedName));
                    }
                }
            }
        }

        var typeNamesByPackage = groupTypeNamesByPackage(returnMap);

        var packageTree = buildPackageTree(typeNamesByPackage);
        return new ProjectAnalysisResponse(warnings, returnMap, typeNamesByPackage, packageTree, endpointResponseList,
                methodCallResponseList, fieldDependencyResponseList, methodDependencyResponseList,
                constructorDependencyResponseList, inheritanceDependencyResponseList);
    }

    private List<MethodResponse> extractMethods(TypeDeclaration<?> type) {

        List<MethodResponse> methodResponses = new ArrayList<>();

        var methodDeclarations = type.getMembers().stream()
                .filter(BodyDeclaration::isMethodDeclaration)
                .map(BodyDeclaration::asMethodDeclaration)
                .toList();

        for (var method : methodDeclarations) {
            List<ParameterResponse> parameters = new ArrayList<>();
            List<String> methodAnnotations = method.getAnnotations().stream()
                    .map(Node::toString).toList();
            var methodName = method.getNameAsString();
            var methodReturnType = method.getTypeAsString();
            var methodReferencedType = extractReferencedTypes(method.getType());
            var methodSignature = method.getSignature().asString();

            for (var u = 0; u < method.getParameters().size(); u++) {
                var declarationParameter = method.getParameter(u);
                List<String> parameterAnnotations = declarationParameter.getAnnotations().stream()
                        .map(Node::toString).toList();
                var parameter = declarationParameter.getName().toString();
                var parameterType = declarationParameter.getType().toString();
                var parameterReferencedTypes = extractReferencedTypes(declarationParameter.getType());
                parameters.add(new ParameterResponse(parameter, parameterType, parameterAnnotations, parameterReferencedTypes));
            }

            methodResponses.add(new MethodResponse(methodName, methodSignature, methodReturnType,
                    methodAnnotations, parameters, methodReferencedType));
        }

        if (type.isRecordDeclaration()) {
            var recordDeclaration = type.asRecordDeclaration();

            for (var field : recordDeclaration.getParameters()) {

                if (methodResponses.stream().anyMatch(i ->
                        i.name().equals(field.getNameAsString()) && i.parameters().isEmpty())) {
                    continue;
                }
                var annotations = field.getAnnotations().stream()
                        .map(Node::toString).toList();
                var referencedTypes = extractReferencedTypes(field.getType());

                methodResponses.add(new MethodResponse(
                        field.getNameAsString(), field.getNameAsString() + "()",
                        field.getTypeAsString(), annotations, List.of(),referencedTypes));
            }
        }

        if (type.isAnnotationPresent("Data") || type.isAnnotationPresent("Getter") || type.isAnnotationPresent("Value")) {
            for (var field : type.getFields()) {
                if (field.isStatic()) continue;

                for (var variable : field.getVariables()) {
                    var varType = variable.getType();
                    var name = variable.getName().asString();
                    var firstUpperLetter = name.substring(0,1).toUpperCase();
                    var restLetters = name.substring(1);
                    var prefix = "get";
                    String getterName;
                    if (varType.isPrimitiveType() &&
                    varType.asPrimitiveType().getType().equals(PrimitiveType.Primitive.BOOLEAN)) {
                        if (name.startsWith("is") && name.length() > 2 && Character.isUpperCase(name.charAt(2))) {
                            getterName = name;
                        } else {
                            prefix = "is";
                            getterName = prefix + firstUpperLetter + restLetters;
                        }
                    } else {
                        getterName = prefix + firstUpperLetter + restLetters;
                    }

                    if (methodResponses.stream().anyMatch(i ->
                            i.name().equals(getterName) && i.parameters().isEmpty())) {
                        continue;
                    }

                    methodResponses.add(new MethodResponse(
                            getterName, getterName + "()",
                            variable.getTypeAsString(), List.of(), List.of(),List.of()));
                }
            }
        }

        if (type.isAnnotationPresent("Data") || type.isAnnotationPresent("Setter") ) {
            for (var field : type.getFields()) {
                if (field.isStatic() || field.isFinal()) continue;

                for (var variable : field.getVariables()) {
                    var varType = variable.getType();
                    var name = variable.getName().asString();
                    var firstUpperLetter = name.substring(0,1).toUpperCase();
                    var restLetters = name.substring(1);
                    String setterName;
                    if (varType.isPrimitiveType() &&
                            varType.asPrimitiveType().getType().equals(PrimitiveType.Primitive.BOOLEAN) &&
                            name.startsWith("is") && name.length() > 2
                            && Character.isUpperCase(name.charAt(2))) {

                            setterName = "set" + name.substring(2);
                    } else {
                        setterName = "set" + firstUpperLetter + restLetters;
                    }

                    if (methodResponses.stream().anyMatch(i ->
                            i.name().equals(setterName) && i.parameters().size() == 1)) {
                        continue;
                    }

                    var typeForSignature = varType.isClassOrInterfaceType() ?
                            varType.asClassOrInterfaceType().getNameWithScope() : varType.asString();
                    methodResponses.add(new MethodResponse(
                            setterName, setterName + "(" + typeForSignature + ")",
                            "void", List.of(),
                            List.of(new ParameterResponse(name, varType.asString(), List.of(), List.of())),
                            List.of()));
                }
            }
        }
        //
        var repoNames = Set.of("JpaRepository", "CrudRepository", "ListCrudRepository", "PagingAndSortingRepository");
        if (type.isClassOrInterfaceDeclaration() && type.asClassOrInterfaceDeclaration().isInterface()) {
            var typeArgs = type.asClassOrInterfaceDeclaration()
                    .getExtendedTypes()
                    .stream()
                    .filter(i-> repoNames.contains(i.getNameAsString()))
                    .findFirst().flatMap(ClassOrInterfaceType::getTypeArguments);
            if (typeArgs.isPresent() && typeArgs.get().size() == 2) {
                var entity = typeArgs.get().get(0).asString();
                var id = typeArgs.get().get(1).asString();

                addIfMissing(methodResponses, generatedMethod("save", entity, "entity",entity));
                addIfMissing(methodResponses, generatedMethod("saveAndFlush", entity, "entity", entity));
                addIfMissing(methodResponses, generatedMethod("delete", "void", "entity", entity));
                addIfMissing(methodResponses, generatedMethod("findById", "Optional<"+entity+">", "id", id));
                addIfMissing(methodResponses, generatedMethod("findAll", "List<"+entity+">", null, null));
                addIfMissing(methodResponses, generatedMethod("existsById", "boolean", "id", id));
                addIfMissing(methodResponses, generatedMethod("deleteById", "void", "id", id));
                addIfMissing(methodResponses, generatedMethod("count", "long", null, null));
            }
        }
        return methodResponses;
    }

    private List<FieldResponse> extractFields(TypeDeclaration<?> type) {

        List<FieldResponse> fieldResponses = new ArrayList<>();

        var fieldDeclarations = type.getMembers().stream()
                .filter(BodyDeclaration::isFieldDeclaration)
                .map(BodyDeclaration::asFieldDeclaration)
                .toList();

        if (type.isRecordDeclaration()) {
            var recordDeclaration = type.asRecordDeclaration();

            for (var u = 0; u < recordDeclaration.getParameters().size(); u++) {
                var variable = recordDeclaration.getParameter(u);
                List<String> annotations = variable.getAnnotations().stream()
                        .map(Node::toString).toList();
                var fieldName = variable.getName().toString();
                var fieldType = variable.getType().toString();
                var referencedType = extractReferencedTypes(variable.getType());
                fieldResponses.add(new FieldResponse(fieldName, fieldType, annotations, referencedType));
            }
        }
        for (var field : fieldDeclarations) {
            List<String> annotations = field.getAnnotations().stream()
                    .map(Node::toString).toList();
            for (var u = 0; u < field.getVariables().size(); u++) {
                var variable = field.getVariable(u);
                var fieldName = variable.getName().toString();
                var fieldType = variable.getType().toString();
                var referencedType = extractReferencedTypes(variable.getType());
                fieldResponses.add(new FieldResponse(fieldName, fieldType, annotations, referencedType));
            }
        }
        return fieldResponses;
    }

    private List<ConstructorResponse> extractConstructors(TypeDeclaration<?> type) {
        List<ConstructorResponse> constructorResponses = new ArrayList<>();

        var constructionDeclaration = type.getMembers()
                .stream()
                .filter(BodyDeclaration::isConstructorDeclaration)
                .map(BodyDeclaration::asConstructorDeclaration)
                .toList();

        for (var constructor : constructionDeclaration) {
            List<ParameterResponse> parameters = new ArrayList<>();
            List<String> constructorAnnotations = constructor.getAnnotations().stream()
                    .map(Node::toString).toList();
            var constructorName = constructor.getNameAsString();

            for (var u = 0; u < constructor.getParameters().size(); u++) {
                var declarationParameter = constructor.getParameter(u);
                List<String> parameterAnnotations = declarationParameter.getAnnotations().stream()
                        .map(Node::toString).toList();
                var parameter = declarationParameter.getName().toString();
                var parameterType = declarationParameter.getType().toString();
                var referencedTypes = extractReferencedTypes(declarationParameter.getType());
                parameters.add(new ParameterResponse(parameter, parameterType, parameterAnnotations, referencedTypes));
            }
            constructorResponses.add(new ConstructorResponse(constructorName, constructorAnnotations, parameters));
        }
        return constructorResponses;
    }

    private InheritanceResponse extractInheritance(TypeDeclaration<?> type) {
        List<InheritanceTypeResponse> extendedTypes = new ArrayList<>();
        List<InheritanceTypeResponse> implementedTypes = new ArrayList<>();

        if (type.isClassOrInterfaceDeclaration()) {
            var inheritanceDeclaration = type.asClassOrInterfaceDeclaration();
            var extended = inheritanceDeclaration.getExtendedTypes()
                    .stream()
                    .toList();

            for (var ext : extended) {
                extendedTypes.add(new InheritanceTypeResponse(
                        ext.toString(), ext.getNameAsString(), extractReferencedTypes(ext))
                );
            }
            var implemented = inheritanceDeclaration.getImplementedTypes()
                    .stream()
                    .toList();

            for (var impl : implemented) {
                implementedTypes.add(new InheritanceTypeResponse(
                        impl.toString(), impl.getNameAsString(),extractReferencedTypes(impl)));
            }
        }
        if (type.isRecordDeclaration()) {
            var inheritanceDeclaration = type.asRecordDeclaration();
            var implemented = inheritanceDeclaration.getImplementedTypes()
                    .stream()
                    .toList();

            for (var impl : implemented) {
                implementedTypes.add(new InheritanceTypeResponse(
                        impl.toString(), impl.getNameAsString(),extractReferencedTypes(impl)));
            }
        }
        if (type.isEnumDeclaration()) {
            var inheritanceDeclaration = type.asEnumDeclaration();
            var implemented = inheritanceDeclaration.getImplementedTypes()
                    .stream()
                    .toList();

            for (var impl : implemented) {
                implementedTypes.add(new InheritanceTypeResponse(
                        impl.toString(), impl.getNameAsString(),extractReferencedTypes(impl)));
            }
        }
        return new InheritanceResponse(extendedTypes, implementedTypes);
    }

    private List<ImportResponse> extractImports(CompilationUnit compilationUnit) {
        List<ImportResponse> importResponses = new ArrayList<>();

        var importDeclaration = compilationUnit.getImports();
        for (var singleImport : importDeclaration) {
            importResponses.add(new ImportResponse(
                    singleImport.getNameAsString(),
                    singleImport.isStatic(),
                    singleImport.isAsterisk(),
                    singleImport.isModule()));
        }
        return importResponses;
    }

    private Map<String, ClassDetailsResponse> mapTypesByQualifiedName(Map<String, List<ClassDetailsResponse>> map) {
        Map<String, ClassDetailsResponse> returnMap = new HashMap<>();

        for (var value : map.values()) {
            for (var type : value) {
                String fullName;
                if (type.packageName() == null || type.packageName().isEmpty()) {
                    fullName = type.classNameResponse().className();
                } else {
                    fullName = type.packageName() + "." + type.classNameResponse().className();
                }
                returnMap.put(fullName, type);
            }
        }
        return returnMap;
    }

    private List<FieldDependencyResponse> resolveFieldDependencies(ClassDetailsResponse sourceType,
                                                                   List<ImportResponse> imports,
                                                                   Map<String, ClassDetailsResponse> typesByQualifiedName,
                                                                   String sourceTypeName) {

        List<FieldDependencyResponse> returnList = new ArrayList<>();
        for (var field : sourceType.fieldResponseList()) {
            for (var referencedType : field.referencedTypes()) {
                var targetType = resolveProjectTypeName(referencedType, sourceType.packageName(), imports, typesByQualifiedName);
                if (targetType == null) {
                    continue;
                }
                returnList.add(new FieldDependencyResponse(sourceTypeName, field.name(), targetType));
            }
        }
        return returnList;
    }

    private List<String> extractReferencedTypes(Type type) {
        return type.findAll(ClassOrInterfaceType.class)
                .stream()
                .map(NodeWithSimpleName::getNameAsString)
                .distinct().toList();
    }

    private String resolveProjectTypeName(String referencedTypeName, String packageName, List<ImportResponse> imports,
                                          Map<String, ClassDetailsResponse> typesByQualifiedName) {

        var foundImport = imports.stream()
                .filter(i -> !i.isAsterisk() && !i.isModule() && !i.isStatic())
                .map(ImportResponse::name)
                .filter(name -> name.endsWith("." + referencedTypeName)).findFirst().orElse(null);

        String name;
        if (foundImport == null) {
            if (packageName == null || packageName.isEmpty()) {
                name = referencedTypeName;
            } else {
                name = packageName + "." + referencedTypeName;
            }
            if (typesByQualifiedName.containsKey(name)) {
                return name;
            }
        }

        if (typesByQualifiedName.containsKey(foundImport)) {
            return foundImport;
        }

        var foundImportAsterisk = imports.stream()
                .filter(i -> i.isAsterisk() && !i.isModule() && !i.isStatic())
                .map(ImportResponse::name).toList();
        for (var importAsterisk : foundImportAsterisk) {
            importAsterisk += "." + referencedTypeName;
            if (typesByQualifiedName.containsKey(importAsterisk)) {
                return importAsterisk;
            }
        }
        return null;
    }

    private List<MethodDependencyResponse> resolveMethodDependencies(ClassDetailsResponse sourceType, List<ImportResponse> imports,
                                                                     Map<String, ClassDetailsResponse> typesByQualifiedName,
                                                                     String sourceTypeName) {
        List<MethodDependencyResponse> returnList = new ArrayList<>();

        for (var method : sourceType.methodResponseList()) {
            for (var methodReferenceType : method.returnReferencedTypes()) {
                var targetType = resolveProjectTypeName(methodReferenceType, sourceType.packageName(),
                        imports, typesByQualifiedName);
                if (targetType == null) continue;

                returnList.add(new MethodDependencyResponse(sourceTypeName, method.name(),
                        method.signature(),
                        targetType, MethodDependencyKind.RETURN_TYPE, null));
            }

            for (var methodParameters : method.parameters()) {
                for (var parameter : methodParameters.referencedTypes()) {
                    var targetType = resolveProjectTypeName(parameter, sourceType.packageName(),
                            imports, typesByQualifiedName);

                    if (targetType == null) continue;

                    returnList.add(new MethodDependencyResponse(sourceTypeName, method.name(), method.signature(),
                            targetType, MethodDependencyKind.PARAMETER_TYPE, methodParameters.name()));
                }
            }
        }
        return returnList;
    }

    private List<ConstructorDependencyResponse> resolveConstructorDependencies(ClassDetailsResponse sourceType, List<ImportResponse> imports,
                                                                               Map<String, ClassDetailsResponse> typesByQualifiedName,
                                                                               String sourceTypeName) {
        List<ConstructorDependencyResponse> returnList = new ArrayList<>();

        for (var constructor : sourceType.constructorResponseList()) {
            for (var constructorParameters : constructor.parameters()) {
                for (var parameter : constructorParameters.referencedTypes()) {
                    var targetType = resolveProjectTypeName(parameter, sourceType.packageName(),
                            imports, typesByQualifiedName);

                    if (targetType == null) continue;

                    returnList.add(new ConstructorDependencyResponse(sourceTypeName, constructor.name(),
                            targetType, constructorParameters.name()));
                }
            }
        }
        return returnList;
    }

    private List<InheritanceDependencyResponse> resolveInheritanceDependencies(ClassDetailsResponse sourceType, List<ImportResponse> imports,
                                                                               Map<String, ClassDetailsResponse> typesByQualifiedName,
                                                                               String sourceTypeName) {
        List<InheritanceDependencyResponse> returnList = new ArrayList<>();

        for (var extended : sourceType.inheritanceResponse().extendedTypes()) {
            var targetType = resolveProjectTypeName(extended.baseType(), sourceType.packageName(), imports, typesByQualifiedName);
            if (targetType == null) continue;
            returnList.add(new InheritanceDependencyResponse(sourceTypeName, targetType, InheritanceDependencyKind.EXTENDS));

        }
        for (var implemented : sourceType.inheritanceResponse().implementedTypes()) {
            var targetType = resolveProjectTypeName(implemented.baseType(), sourceType.packageName(), imports, typesByQualifiedName);
            if (targetType == null) continue;
            returnList.add(new InheritanceDependencyResponse(sourceTypeName, targetType, InheritanceDependencyKind.IMPLEMENTS));
        }
        return returnList;
    }

    private Map<String, List<String>> groupTypeNamesByPackage(Map<String, List<ClassDetailsResponse>> map) {
        Map<String, List<String>> returnMap = new HashMap<>();

        for (var values : map.values()) {
            for (var value : values) {
                var packageName = value.packageName();
                if (packageName == null) {
                    packageName = "";
                }
                var className = value.classNameResponse().className();
                if (returnMap.containsKey(packageName)) {
                    var returnMapFound = returnMap.get(packageName);
                    returnMapFound.add(className);
                } else {
                    List<String> newList = new ArrayList<>();
                    newList.add(className);
                    returnMap.put(packageName, newList);
                }
            }
        }
        return returnMap;
    }

    private List<PackageNodeResponse> buildPackageTree(Map<String, List<String>> typesByPackage) {
        List<PackageNodeResponse> packageNodeResponses = new ArrayList<>();
        Map<String, PackageNodeResponse> nodesByFullName = new HashMap<>();

        for (var entry : typesByPackage.entrySet()) {
            var packageName = entry.getKey();
            var typeNames = entry.getValue();
            var splitPackages = packageName.split("\\.");
            String currentFullName = "";
            PackageNodeResponse parent = null;

            for (int i = 0; i < splitPackages.length; i++) {
                var split = splitPackages[i];
                if (i == 0) {
                    currentFullName += split;
                } else {
                    currentFullName += "." + split;
                }
                var node = nodesByFullName.get(currentFullName);
                if (node == null) {
                    node = new PackageNodeResponse(split, currentFullName, new ArrayList<>(), new ArrayList<>());
                    nodesByFullName.put(currentFullName, node);
                    if (parent == null) {
                        packageNodeResponses.add(node);
                    } else {
                        parent.subPackages().add(node);
                    }
                }
                parent = node;
            }
            if (parent != null) {
                parent.typeNames().addAll(typeNames);
            }
        }
        return packageNodeResponses;
    }

    private List<EndpointResponse> extractEndpoints(TypeDeclaration<?> type, String declaringTypeName) {
        List<EndpointResponse> returnList = new ArrayList<>();

        var classRequestMapping = type.getAnnotations().stream().filter(string ->
                string.getName().getIdentifier().equals("RequestMapping")).findFirst().orElse(null);

        var classPaths = extractMappingPaths(classRequestMapping);

        var methodDeclaration = type.getMembers().stream()
                .filter(BodyDeclaration::isMethodDeclaration)
                .map(BodyDeclaration::asMethodDeclaration).toList();

        for (var method : methodDeclaration) {

            var methodAnnotations = method.getAnnotations().stream()
                    .filter(i->i.getName().getIdentifier().endsWith("Mapping"))
                    .toList();
            for (var methodAnn : methodAnnotations) {
                var httpMethodKind = extractHttpMethods(methodAnn);

                var methodPaths = extractMappingPaths(methodAnn);

                for (var classPath : classPaths) {
                    for (var methodPath : methodPaths) {
                        String fullPath;
                        if (classPath.isBlank() && methodPath.isBlank()) {
                            fullPath = "/";
                        }
                        else if (classPath.endsWith("/") && methodPath.startsWith("/")) {
                            fullPath = classPath + methodPath.replaceFirst("/", "");
                        }
                        else if ((!classPath.endsWith("/") && !methodPath.startsWith("/")) && !methodPath.isBlank()) {
                            fullPath = classPath + "/" + methodPath;
                        }
                        else {
                            fullPath = classPath + methodPath;
                        }

                        if (!fullPath.startsWith("/")) {
                            fullPath = "/" + fullPath;
                        }

                        for (var http : httpMethodKind) {
                            returnList.add(new EndpointResponse(declaringTypeName, method.getNameAsString(),
                                    method.getSignature().asString(), http, classPath, methodPath, fullPath,
                                    List.of(), List.of()));
                        }


                    }
                }
            }
        }

        return returnList;
    }

    private List<String> extractMappingPaths(AnnotationExpr annotationExpr) {
        List<String> returnList = new ArrayList<>();

        if (annotationExpr == null || annotationExpr.isMarkerAnnotationExpr()) {
            returnList.add("");
            return returnList;
        }
        if (annotationExpr.isSingleMemberAnnotationExpr()) {
            var singleMember = annotationExpr.asSingleMemberAnnotationExpr().getMemberValue();
            if (singleMember.isStringLiteralExpr()) {
                returnList.add(singleMember.asStringLiteralExpr().getValue());
            }

            if (singleMember.isArrayInitializerExpr()) {
                var multipleMembers = singleMember.asArrayInitializerExpr().getValues();
                for (var member : multipleMembers) {
                    if (member.isStringLiteralExpr()) {
                        returnList.add(member.asStringLiteralExpr().getValue());
                    }
                }
            }
        }
        if (annotationExpr.isNormalAnnotationExpr()) {
            var normalMembers = annotationExpr.asNormalAnnotationExpr().getPairs()
                    .stream()
                    .filter(name -> name.getName().asString().equals("path") ||
                            name.getName().asString().equals("value"))
                    .toList();
            if (normalMembers.isEmpty()) {
                returnList.add("");
            }
            for (var pair : normalMembers) {
                if (pair.getValue().isStringLiteralExpr()) {
                    returnList.add(pair.getValue().asStringLiteralExpr().getValue());
                }
                if (pair.getValue().isArrayInitializerExpr()) {
                    var multipleMembers = pair.getValue().asArrayInitializerExpr().getValues();
                    for (var member : multipleMembers) {
                        if (member.isStringLiteralExpr()) {
                            returnList.add(member.asStringLiteralExpr().getValue());
                        }
                    }
                }
            }
        }
        return returnList;
    }

    private List<HttpMethodKind> extractHttpMethods(AnnotationExpr annotationExpr) {
        List<HttpMethodKind> returnList = new ArrayList<>();
        switch (annotationExpr.getName().getIdentifier()) {
            case "GetMapping" -> returnList.add(HttpMethodKind.GET);
            case "PostMapping" -> returnList.add(HttpMethodKind.POST);
            case "PutMapping" -> returnList.add(HttpMethodKind.PUT);
            case "PatchMapping" -> returnList.add(HttpMethodKind.PATCH);
            case "DeleteMapping" -> returnList.add(HttpMethodKind.DELETE);
            case "RequestMapping" -> {
                if (!annotationExpr.isNormalAnnotationExpr()) returnList.add(HttpMethodKind.ANY);
                if (annotationExpr.isNormalAnnotationExpr()) {
                    var normals = annotationExpr.asNormalAnnotationExpr().getPairs()
                            .stream()
                            .filter(name -> name.getName().asString().equals("method"))
                            .toList();
                    if (normals.isEmpty()) returnList.add(HttpMethodKind.ANY);
                    for (var normal : normals) {
                        if (normal.getValue().isFieldAccessExpr()) {
                            var field = normal.getValue().asFieldAccessExpr().getNameAsString();
                            switch (field) {
                                case "GET" -> returnList.add(HttpMethodKind.GET);
                                case "POST" -> returnList.add(HttpMethodKind.POST);
                                case "PUT" -> returnList.add(HttpMethodKind.PUT);
                                case "PATCH" -> returnList.add(HttpMethodKind.PATCH);
                                case "DELETE" -> returnList.add(HttpMethodKind.DELETE);
                                case "HEAD" -> returnList.add(HttpMethodKind.HEAD);
                                case "OPTIONS" -> returnList.add(HttpMethodKind.OPTIONS);
                                case "TRACE" -> returnList.add(HttpMethodKind.TRACE);
                            }
                        }
                        if (normal.getValue().isArrayInitializerExpr()) {
                            var arrayNormals = normal.getValue().asArrayInitializerExpr().getValues();
                            if (arrayNormals.isEmpty()) returnList.add(HttpMethodKind.ANY);
                            for (var arrNormal : arrayNormals) {
                                if (arrNormal.isFieldAccessExpr()) {
                                    var field = arrNormal.asFieldAccessExpr().getNameAsString();
                                    switch (field) {
                                        case "GET" -> returnList.add(HttpMethodKind.GET);
                                        case "POST" -> returnList.add(HttpMethodKind.POST);
                                        case "PUT" -> returnList.add(HttpMethodKind.PUT);
                                        case "PATCH" -> returnList.add(HttpMethodKind.PATCH);
                                        case "DELETE" -> returnList.add(HttpMethodKind.DELETE);
                                        case "HEAD" -> returnList.add(HttpMethodKind.HEAD);
                                        case "OPTIONS" -> returnList.add(HttpMethodKind.OPTIONS);
                                        case "TRACE" -> returnList.add(HttpMethodKind.TRACE);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            default -> {
                return List.of();
            }
        }
        return returnList;
    }

    private List<CallSite> extractMethodCalls(TypeDeclaration<?> type, String declaringTypeName) {
        List<CallSite> returnList = new ArrayList<>();
        var methods = type.getMethods();
        for (var method : methods) {
            var methodBody = method.getBody();
            if (methodBody.isEmpty()) continue;
            var blockStmt = methodBody.get();

            var methodCallExprs = blockStmt.findAll(MethodCallExpr.class);
            for (var methodCall : methodCallExprs) {
                var calledMethodName = methodCall.getNameAsString();
                var scope = methodCall.getScope().orElse(null);
                var respScope = methodCall.getScope().map(Node::toString).orElse(null);
                String scopeName = null;
                boolean foundLocal = false;
                if (scope != null && scope.isNameExpr()) {
                    scopeName = scope.asNameExpr().getNameAsString();
                }
                String scopeTypeName = null;
                for (var u = 0; u < method.getParameters().size(); u++) {
                    var declarationParameter = method.getParameter(u);
                    if (declarationParameter.getNameAsString().equals(scopeName)) {
                        scopeTypeName = declarationParameter.getType().asString();
                        break;
                    }
                }

                if (scopeName != null && scopeTypeName == null) {
                    var declarators = blockStmt.findAll(VariableDeclarator.class);
                    for (var decl : declarators) {
                        if (decl.getNameAsString().equals(scopeName)) {
                            foundLocal = true;
                            if (!decl.getTypeAsString().equals("var")) {
                                scopeTypeName = decl.getTypeAsString();
                            } else if (decl.getInitializer().isPresent() &&
                                    decl.getInitializer().get().isObjectCreationExpr()) {
                                scopeTypeName = decl.getInitializer().get().asObjectCreationExpr().getTypeAsString();
                            }
                            break;
                        }
                    }
                }
                if (scopeName != null && scopeTypeName == null && !foundLocal) {
                    for (var field : type.getFields()) {
                        for (var variable : field.getVariables()) {
                            if (variable.getNameAsString().equals(scopeName)) {
                                scopeTypeName = variable.getType().asString();

                            }
                        }
                    }
                }
                var argumentCount = methodCall.getArguments().size();
                returnList.add(new CallSite(methodCall, new MethodCallResponse(declaringTypeName, method.getSignature().asString(),
                        calledMethodName, respScope, scopeTypeName,null, null, argumentCount)));
            }
        }
        return returnList;
    }

    private MethodCallResponse resolveCall(CallSite call, String sourceTypeName, String packageName,
                                           List<ImportResponse> imports, Map<String, ClassDetailsResponse> typesByQualifiedName,
                                           IdentityHashMap<MethodCallExpr, CallSite> sitesByExpr,
                                           IdentityHashMap<MethodCallExpr, MethodCallResponse> resolvedByExpr,
                                           Map<String, List<ImportResponse>> importsByQualifiedName) {

        if (resolvedByExpr.containsKey(call.methodCallExpr())) {
            return resolvedByExpr.get(call.methodCallExpr());
        }

        var callRsp = call.methodCallResponse();
        var scopeType = callRsp.scopeType();
        String targetType = null;
        String methodSignature = null;

        if (callRsp.scopeType() != null) {
            targetType = resolveProjectTypeName(callRsp.scopeType(), packageName, imports,typesByQualifiedName);
        }
        else if (callRsp.scope() == null || callRsp.scope().equals("this")) {
            if (Objects.equals(callRsp.scope(), "this") || typesByQualifiedName.get(callRsp.sourceType()).methodResponseList().stream().anyMatch(i->i.name().equals(callRsp.calledMethodName()))) {
                targetType = sourceTypeName;
            } else {
                String owner = null;
                String asteriskOwner = null;
                for (var staticImport : imports.stream().filter(ImportResponse::isStatic).toList()) {
                    if (staticImport.name().endsWith("." + callRsp.calledMethodName())) {
                        owner = staticImport.name().substring(0, staticImport.name().lastIndexOf("."));
                        break;
                    } else if (staticImport.isAsterisk()) {
                        asteriskOwner = staticImport.name();
                    }
                }
                if (owner == null) {
                    owner = asteriskOwner;
                }
                if (owner != null) {
                    if (typesByQualifiedName.containsKey(owner)) {
                        targetType = owner;
                    } else {
                        scopeType = owner.substring(owner.lastIndexOf(".") + 1);
                    }
                } else {
                    targetType = sourceTypeName;
                }
            }
        }
        else if (call.methodCallExpr().getScope().isPresent() && call.methodCallExpr().getScope().get().isMethodCallExpr()) {
            var innerExp = call.methodCallExpr().getScope().get().asMethodCallExpr();
            targetType = typeOfCall(innerExp, sourceTypeName, packageName, imports, typesByQualifiedName, sitesByExpr, resolvedByExpr, importsByQualifiedName);
        }
        else if(call.methodCallExpr().getScope().isPresent() && call.methodCallExpr().getScope().get().isNameExpr()) {
            var nameExpr = call.methodCallExpr().getScope().get().asNameExpr();
            var enclosingMethod = nameExpr.findAncestor(MethodDeclaration.class).orElse(null);

            if (enclosingMethod != null) {
                var declarator = enclosingMethod.findAll(VariableDeclarator.class).stream().filter(i ->
                        i.getNameAsString().equals(nameExpr.getNameAsString())).findFirst();
                var initializer = declarator.flatMap(VariableDeclarator::getInitializer);
                var ancestor = declarator.flatMap(variableDeclarator -> variableDeclarator.findAncestor(ForEachStmt.class));

                if (initializer.isPresent() && initializer.get().isMethodCallExpr()) {
                    var initCall = initializer.get().asMethodCallExpr();
                    targetType = typeOfCall(initCall, sourceTypeName, packageName, imports, typesByQualifiedName, sitesByExpr, resolvedByExpr, importsByQualifiedName);
                }
                else if (declarator.isPresent() && (ancestor.isPresent() && ancestor.get().getVariableDeclarator().equals(declarator.get()))) {
                    if (ancestor.get().getIterable().isNameExpr()) {
                        var ancestorName = ancestor.get().getIterable().asNameExpr();
                        var iterableDeclarator = enclosingMethod.findAll(VariableDeclarator.class).stream().filter(i ->
                                Objects.equals(i.getNameAsString(), ancestorName.getNameAsString())).findFirst();

                        var iterableInitializer = iterableDeclarator.flatMap(VariableDeclarator::getInitializer);
                        String elementType = null;
                        if (iterableInitializer.isPresent() && iterableInitializer.get().isMethodCallExpr()) {
                            var iterableCall = iterableInitializer.get().asMethodCallExpr();
                            if (sitesByExpr.containsKey(iterableCall)) {
                                var foundIterable = sitesByExpr.get(iterableCall);
                                var resolved = resolveCall(foundIterable, sourceTypeName, packageName, imports, typesByQualifiedName, sitesByExpr, resolvedByExpr, importsByQualifiedName);
                                var returnTypeText = rawReturnType(resolved, typesByQualifiedName);
                                var type = typeArgument(returnTypeText);
                                targetType = resolveInClassContext(type, resolved.targetType(), importsByQualifiedName, typesByQualifiedName);
                            }
                        }
                        else if (iterableDeclarator.isPresent() && !iterableDeclarator.get().getTypeAsString().equals("var")) {
                            elementType = typeArgument(iterableDeclarator.get().getTypeAsString());
                        }
                        else if (iterableDeclarator.isEmpty()) {
                            var parameter = enclosingMethod.getParameterByName(ancestorName.getNameAsString());
                            if (parameter.isPresent()) {
                                elementType = typeArgument(parameter.get().getTypeAsString());
                            }
                        }
                        if (elementType != null) {
                            targetType = resolveProjectTypeName(elementType, packageName, imports, typesByQualifiedName);
                        }

                    }
                }
            }
        }
        if (targetType != null) {
            MethodResponse methodResponse = null;
            var sameMethodsCount = 0;
            String currentlySearchedClass = targetType;
            Set<String> searched = new HashSet<>();

            while (currentlySearchedClass != null) {
                if (!searched.add(currentlySearchedClass)) break;
                var targetClass = typesByQualifiedName.get(currentlySearchedClass);
                for (var targetClassMethod : targetClass.methodResponseList()) {
                    if (callRsp.argumentCount() == targetClassMethod.parameters().size() &&
                            callRsp.calledMethodName().equals(targetClassMethod.name())) {
                        sameMethodsCount += 1;
                        methodResponse = targetClassMethod;
                    }
                }
                if (sameMethodsCount == 0) {
                    currentlySearchedClass = superClassOf(currentlySearchedClass, importsByQualifiedName, typesByQualifiedName);
                }
                else if (sameMethodsCount == 1) {
                    methodSignature = methodResponse.signature();
                    targetType = currentlySearchedClass;
                    break;
                }
                else {
                    break;
                }
            }

        }

        var methodCallResponse = new MethodCallResponse(
                callRsp.sourceType(), callRsp.sourceMethodSignature(), callRsp.calledMethodName(),
                callRsp.scope(), scopeType, targetType, methodSignature, callRsp.argumentCount());
        resolvedByExpr.put(call.methodCallExpr(), methodCallResponse);
        return methodCallResponse;
    }

    private String resolveReturnType(MethodCallResponse resolvedCall,
                                     Map<String, ClassDetailsResponse> typesByQualifiedName,
                                     Map<String, List<ImportResponse>> importsByQualifiedName) {

        var methodReturnType = rawReturnType(resolvedCall, typesByQualifiedName);
        if (methodReturnType == null) return null;

        return resolveInClassContext(methodReturnType, resolvedCall.targetType(), importsByQualifiedName, typesByQualifiedName);
    }

    private MethodResponse generatedMethod(String methodName, String returnType,
                                           String parameterName, String parameterType) {
        String signature;
        List<ParameterResponse> parameterResponse = new ArrayList<>();
        if (parameterType == null) {
            signature = methodName + "()";
        } else {
            signature = methodName + "(" + parameterType + ")";
            parameterResponse.add(new ParameterResponse(parameterName, parameterType,List.of(), List.of()));
        }

        return new MethodResponse(methodName, signature, returnType, List.of(), parameterResponse, List.of());
    }

    private void addIfMissing(List<MethodResponse> methodResponses, MethodResponse newResponse) {
        var exists = methodResponses.stream().anyMatch(i ->
                Objects.equals(i.name(), newResponse.name()) &&
                i.parameters().size() == newResponse.parameters().size());
        if (!exists) methodResponses.add(newResponse);
    }

    private String typeArgument(String text) {
        if (text == null) return null;

        var firstBrace = text.indexOf("<");
        var lastBrace = text.lastIndexOf(">");
        if (firstBrace == -1 || lastBrace == -1) return null;
        var inside = text.substring(firstBrace+1, lastBrace).trim();
        if (inside.contains(",") || inside.isEmpty()) return null;

        return inside;
    }

    private String rawReturnType(MethodCallResponse resolvedCall,
                                 Map<String, ClassDetailsResponse> typesByQualifiedName) {
        if (resolvedCall.targetType() == null || resolvedCall.targetMethodSignature() == null) {
            return null;
        }

        var innerClass = typesByQualifiedName.get(resolvedCall.targetType());

        return innerClass.methodResponseList().stream()
                .filter(i-> Objects.equals(i.signature(), resolvedCall.targetMethodSignature()))
                .map(MethodResponse::returnType).findFirst().orElse(null);
    }

    private String typeOfCall(MethodCallExpr methodCallExpr, String sourceTypeName, String packageName,
                              List<ImportResponse> imports, Map<String, ClassDetailsResponse> typesByQualifiedName,
                              IdentityHashMap<MethodCallExpr, CallSite> sitesByExpr,
                              IdentityHashMap<MethodCallExpr, MethodCallResponse> resolvedByExpr,
                              Map<String, List<ImportResponse>> importsByQualifiedName) {

        var callNames = Set.of("orElseThrow", "orElse", "orElseGet", "get");
        if (callNames.contains(methodCallExpr.getNameAsString()) && methodCallExpr.getScope().isPresent() &&
                methodCallExpr.getScope().get().isMethodCallExpr()) {

            var innerCall = methodCallExpr.getScope().get().asMethodCallExpr();
            if (!sitesByExpr.containsKey(innerCall)) return null;

            var callSite = sitesByExpr.get(innerCall);
            var innerResolved = resolveCall(callSite, sourceTypeName, packageName, imports,
                    typesByQualifiedName, sitesByExpr, resolvedByExpr, importsByQualifiedName);

            var raw = rawReturnType(innerResolved, typesByQualifiedName);
            if (raw != null && raw.startsWith("Optional<")) {
                var inside = typeArgument(raw);

                return resolveInClassContext(inside, innerResolved.targetType(), importsByQualifiedName, typesByQualifiedName);
            }
        }

        if (!sitesByExpr.containsKey(methodCallExpr)) return null;

        var callSite = sitesByExpr.get(methodCallExpr);
        var methodCallResponse = resolveCall(callSite, sourceTypeName, packageName, imports, typesByQualifiedName,
                sitesByExpr, resolvedByExpr, importsByQualifiedName);

        return resolveReturnType(methodCallResponse, typesByQualifiedName, importsByQualifiedName);
    }

    private String resolveInClassContext(String typeText, String classFullName,
                                         Map<String, List<ImportResponse>> importsByQualifiedName,
                                         Map<String, ClassDetailsResponse> typesByQualifiedName) {
        if (typeText == null || classFullName == null) {
            return null;
        }

        if (!typesByQualifiedName.containsKey(classFullName)) return null;

        var foundClass = typesByQualifiedName.get(classFullName);
        var foundImports = importsByQualifiedName.get(classFullName);
        return resolveProjectTypeName(typeText, foundClass.packageName(), foundImports, typesByQualifiedName);
    }


    private String superClassOf(String classFullName, Map<String, List<ImportResponse>> importsByQualifiedName,
                                Map<String, ClassDetailsResponse> typesByQualifiedName) {
        if (!typesByQualifiedName.containsKey(classFullName)) {
            return null;
        }
        var foundClass = typesByQualifiedName.get(classFullName).inheritanceResponse().extendedTypes();
        if (foundClass.isEmpty()) return null;
        var firstExtType = foundClass.getFirst();
        return resolveInClassContext(firstExtType.baseType(), classFullName, importsByQualifiedName, typesByQualifiedName);
    }
    // TODO add liquibase and flyway support
}