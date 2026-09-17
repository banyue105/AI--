import type { SkillNode } from '../types'

export interface SkillQuestion {
  id: string
  prompt: string
  options: string[]
  answerIndex: number
  explanation: string
}

const questionBank: Record<string, SkillQuestion[]> = {
  python: [
    { id: 'python-1', prompt: 'Python 中哪种数据类型是不可变的？', options: ['list', 'dict', 'tuple', 'set'], answerIndex: 2, explanation: 'tuple 创建后不能增删或替换其中的元素。' },
    { id: 'python-2', prompt: '用于捕获异常的关键字组合是？', options: ['if / else', 'try / except', 'for / while', 'match / case'], answerIndex: 1, explanation: 'try / except 用于执行可能失败的代码并处理异常。' },
    { id: 'python-3', prompt: '创建虚拟环境的常用命令是？', options: ['python -m venv .venv', 'python -m build', 'pip freeze', 'python -m test'], answerIndex: 0, explanation: 'venv 模块用于创建彼此隔离的 Python 环境。' },
  ],
  linux: [
    { id: 'linux-1', prompt: '查看当前工作目录的命令是？', options: ['pwd', 'ps', 'who', 'cat'], answerIndex: 0, explanation: 'pwd 会输出当前工作目录的完整路径。' },
    { id: 'linux-2', prompt: '修改文件权限通常使用哪个命令？', options: ['grep', 'chmod', 'touch', 'kill'], answerIndex: 1, explanation: 'chmod 用于修改文件或目录的访问权限。' },
    { id: 'linux-3', prompt: '查看正在运行进程的常用命令是？', options: ['cp', 'ps', 'mv', 'df'], answerIndex: 1, explanation: 'ps 用于查看当前系统中的进程信息。' },
  ],
  tcpip: [
    { id: 'tcpip-1', prompt: 'TCP 建立连接通常需要几次握手？', options: ['1 次', '2 次', '3 次', '4 次'], answerIndex: 2, explanation: 'TCP 通过三次握手确认双方收发能力并建立连接。' },
    { id: 'tcpip-2', prompt: 'IP 协议主要工作在哪一层？', options: ['应用层', '传输层', '网络层', '数据链路层'], answerIndex: 2, explanation: 'IP 负责网络层的寻址与分组转发。' },
    { id: 'tcpip-3', prompt: '以下哪一个协议通常使用 UDP？', options: ['DNS 查询', 'HTTPS', 'SSH', 'SMTP'], answerIndex: 0, explanation: '常规 DNS 查询通常使用 UDP，需要时也可以切换到 TCP。' },
  ],
  git: [
    { id: 'git-1', prompt: '把工作区改动加入暂存区的命令是？', options: ['git add', 'git push', 'git clone', 'git log'], answerIndex: 0, explanation: 'git add 会把指定改动加入暂存区。' },
    { id: 'git-2', prompt: '创建并切换到新分支的常用命令是？', options: ['git fetch -a', 'git switch -c', 'git merge -d', 'git reset -b'], answerIndex: 1, explanation: 'git switch -c <name> 会创建并切换到新分支。' },
    { id: 'git-3', prompt: '查看提交历史通常使用？', options: ['git log', 'git status', 'git diff --cached', 'git stash'], answerIndex: 0, explanation: 'git log 用于查看当前仓库的提交历史。' },
  ],
  http: [
    { id: 'http-1', prompt: '表示资源未找到的状态码是？', options: ['200', '301', '404', '500'], answerIndex: 2, explanation: '404 Not Found 表示服务器找不到请求的资源。' },
    { id: 'http-2', prompt: '用于创建资源的常见请求方法是？', options: ['GET', 'POST', 'HEAD', 'OPTIONS'], answerIndex: 1, explanation: 'POST 常用于向集合提交数据并创建新资源。' },
    { id: 'http-3', prompt: '以下哪个请求方法通常应具备幂等性？', options: ['PATCH', 'POST', 'PUT', 'CONNECT'], answerIndex: 2, explanation: '按 HTTP 语义，重复执行同一个 PUT 请求应产生相同结果。' },
  ],
  docker: [
    { id: 'docker-1', prompt: '定义镜像构建步骤的文件通常是？', options: ['Dockerfile', 'package.json', 'Makefile.lock', 'container.ini'], answerIndex: 0, explanation: 'Dockerfile 用声明式指令描述镜像构建过程。' },
    { id: 'docker-2', prompt: '查看运行中容器的命令是？', options: ['docker ps', 'docker init', 'docker pull', 'docker build'], answerIndex: 0, explanation: 'docker ps 默认列出当前正在运行的容器。' },
    { id: 'docker-3', prompt: '持久化容器数据更适合使用？', options: ['环境变量', '数据卷', '镜像标签', '端口映射'], answerIndex: 1, explanation: '数据卷的生命周期独立于容器，适合保存持久数据。' },
  ],
}

const genericQuestions: SkillQuestion[] = [
  { id: 'generic-1', prompt: '确认自己掌握一项技能，最可靠的依据是？', options: ['只看过教程', '能在真实任务中独立应用', '收藏了资料', '知道技能名称'], answerIndex: 1, explanation: '真实任务中的独立应用比浏览或记忆更能证明掌握程度。' },
  { id: 'generic-2', prompt: '遇到不确定的技术结论时，应优先怎么做？', options: ['直接猜测', '查阅官方资料并做最小验证', '忽略问题', '只看标题'], answerIndex: 1, explanation: '官方资料与可复现验证能形成更可靠的判断依据。' },
  { id: 'generic-3', prompt: '哪项记录最有助于复盘一次实践？', options: ['只写完成了', '记录目标、过程、结果和问题', '只保留截图', '不做记录'], answerIndex: 1, explanation: '完整记录目标、过程、结果和问题，才能支持后续复盘和能力判断。' },
]

function localQuestions(skill: SkillNode) {
  const normalized = `${skill.id} ${skill.name}`.toLowerCase()
  const key = Object.keys(questionBank).find((candidate) => normalized.includes(candidate))
    ?? (normalized.includes('tcp') ? 'tcpip' : undefined)
    ?? (normalized.includes('容器') ? 'docker' : undefined)
  return structuredClone(key ? questionBank[key] : genericQuestions)
}

export const skillAssessmentService = {
  async getQuestions(skill: SkillNode): Promise<SkillQuestion[]> {
    const controller = new AbortController()
    const timeout = window.setTimeout(() => controller.abort(), 900)
    try {
      const response = await fetch(`/api/v1/ability/skills/${encodeURIComponent(skill.id)}/assessment/questions`, {
        signal: controller.signal,
      })
      if (!response.ok) throw new Error(`API ${response.status}`)
      const result = await response.json() as { questions?: SkillQuestion[] } | SkillQuestion[]
      const questions = Array.isArray(result) ? result : result.questions
      return questions?.length ? questions : localQuestions(skill)
    } catch {
      return localQuestions(skill)
    } finally {
      window.clearTimeout(timeout)
    }
  },
}
