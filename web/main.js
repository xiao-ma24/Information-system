const form = document.querySelector('#login');
const error = document.querySelector('#error');
form.addEventListener('submit', async (event) => {
  event.preventDefault();
  error.textContent = '';
  const button = form.querySelector('button');
  button.disabled = true;
  try {
    const response = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'same-origin',
      body: JSON.stringify(Object.fromEntries(new FormData(form)))
    });
    if (response.ok) {
      location.assign('/portal.html');
      return;
    }
    error.textContent = response.status === 401
      ? '账号或密码不正确，请重试。'
      : '登录服务暂时不可用，请稍后重试。';
  } catch {
    error.textContent = '无法连接登录服务，请检查网络。';
  } finally {
    button.disabled = false;
  }
});
