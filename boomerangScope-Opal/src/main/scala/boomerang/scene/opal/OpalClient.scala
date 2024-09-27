package boomerang.scene.opal

import org.opalj.br.{ClassFile, ClassHierarchy, DeclaredMethod, Field, Method, MethodDescriptor, ObjectType, ReferenceType}
import org.opalj.br.analyses.{DeclaredMethods, DeclaredMethodsKey, Project}
import org.opalj.tac.{AITACode, ComputeTACAIKey, FieldRead, FieldWriteAccessStmt, TACMethodParameter}
import org.opalj.value.ValueInformation

object OpalClient {

  private var project: Option[Project[_]] = None
  private var declaredMethods: Option[DeclaredMethods] = None
  private var tacCodes: Option[Method => AITACode[TACMethodParameter, ValueInformation]] = None

  def init(p: Project[_]): Unit = {
    project = Some(p)
    declaredMethods = Some(p.get(DeclaredMethodsKey))
    tacCodes = Some(p.get(ComputeTACAIKey))
  }

  def getClassHierarchy: ClassHierarchy = project.get.classHierarchy

  def getClassFileForType(objectType: ObjectType): ClassFile = project.get.classFile(objectType).get

  def isApplicationClass(classFile: ClassFile): Boolean = project.get.allProjectClassFiles.toSet.contains(classFile)

  def getDeclaredMethod(method: Method): DeclaredMethod = declaredMethods.get(method)

  def getTacForMethod(method: Method): AITACode[TACMethodParameter, ValueInformation] = tacCodes.get(method)

  def resolveFieldStore(stmt: FieldWriteAccessStmt[_]): Option[Field] = stmt.resolveField(project.get)

  def resolveFieldLoad(expr: FieldRead[_]): Option[Field] = expr.resolveField(project.get)

  def resolveMethodRef(declaringClass: ReferenceType, name: String, methodDescriptor: MethodDescriptor): Option[Method] = project.get.resolveMethodReference(declaringClass, name, methodDescriptor)

}
