const MODULES = [
  { no: '01', name: '学籍管理', desc: '学生学籍、行政班与学籍异动' },
  { no: '02', name: '专业设置及排课', desc: '专业、课程目录与教学班安排' },
  { no: '03', name: '选课及成绩管理', desc: '选课、退选与成绩发布', url: 'http://127.0.0.1:18003/' },
  { no: '04', name: '师资管理', desc: '教职工信息与岗位变动' },
  { no: '05', name: '财务管理', desc: '应收款、缴费与经费收支' },
  { no: '06', name: '社团管理', desc: '社团、活动与公益工时' },
  { no: '07', name: '固定资产管理', desc: '楼宇、房间与设备台账' },
  { no: '08', name: '物资管理', desc: '物资目录、出入库与库存' },
  { no: '09', name: '实验室管理', desc: '实验室状态、容量与预约' },
  { no: '10', name: '科研管理', desc: '项目申报、立项与经费' },
  { no: '11', name: '图书馆管理', desc: '借阅、逾期与罚款' },
  { no: '12', name: '校医院管理', desc: '挂号、就诊与收费' },
  { no: '13', name: '研究生管理', desc: '培养、答辩与学位' },
  { no: '14', name: '后勤管理', desc: '服务请求与任务派工' },
  { no: '15', name: '对外交流管理', desc: '交流项目与学生派出' }
];

const ROLE_LABELS = {
  STUDENT: '学生',
  TEACHER: '教师',
  COUNSELOR: '辅导员',
  STAFF: '教职工'
};

init();

async function init() {
  let user;
  try {
    const response = await fetch('/api/auth/me', { credentials: 'same-origin' });
    if (response.status === 401) {
      location.assign('/');
      return;
    }
    if (!response.ok) {
      throw new Error('me failed');
    }
    user = await response.json();
  } catch {
    showToast('无法连接认证服务，请确认后端已启动');
    return;
  }

  document.querySelector('#user-name').textContent = user.name;
  document.querySelector('#user-role').textContent = ROLE_LABELS[user.role] || user.role;
  document.querySelector('#user-id').textContent = user.business_id || user.user_id;

  document.querySelector('#logout').addEventListener('click', async () => {
    await fetch('/api/auth/logout', { method: 'POST', credentials: 'same-origin' });
    location.assign('/');
  });

  render();
}

function render() {
  const grid = document.querySelector('#modules');
  grid.innerHTML = '';
  for (const module of MODULES) {
    const ready = Boolean(module.url);
    const card = document.createElement('button');
    card.type = 'button';
    card.className = ready ? 'card ready' : 'card';
    card.innerHTML = `
      <span class="state">${ready ? '可用' : '待接入'}</span>
      <span class="no">${module.no}</span>
      <h2>${module.name}</h2>
      <p>${module.desc}</p>`;
    card.addEventListener('click', () => openModule(module));
    grid.appendChild(card);
  }
}

function openModule(module) {
  if (module.url) {
    location.assign(module.url);
    return;
  }
  showToast(`${module.no} ${module.name} 尚未接入，联调后开放`);
}

let toastTimer;
function showToast(message) {
  const toast = document.querySelector('#toast');
  toast.textContent = message;
  toast.classList.add('show');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => toast.classList.remove('show'), 2200);
}
