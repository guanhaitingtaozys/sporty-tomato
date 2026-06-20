/**
 * authUser 云函数 — 微信登录认证
 * 遵循微信 OAuth2.0 授权登录流程（标准 authorization_code 模式）
 *
 * 流程：
 *   1. 用 code 换取 access_token + openId + unionId
 *   2. 刷新 access_token（续期）
 *   3. 获取用户信息（昵称、头像）
 *   4. 返回完整用户数据给客户端
 *
 * 参考：https://developers.weixin.qq.com/doc/oplatform/Mobile_App/WeChat_Login/Development_Guide.html
 */

const https = require('https');

const WECHAT_APP_ID = 'wxa1e2b848fbb48680';
const WECHAT_APP_SECRET = process.env.WECHAT_APP_SECRET || 'YOUR_WECHAT_APP_SECRET_HERE';

function httpGet(url) {
  return new Promise((resolve, reject) => {
    https.get(url, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        try { resolve(JSON.parse(data)); }
        catch (e) { reject(new Error('JSON parse failed: ' + data.substring(0, 200))); }
      });
    }).on('error', (err) => reject(new Error('HTTP error: ' + err.message)));
  });
}

module.exports = async (ctx) => {
  let params;

  try {
    if (ctx.args && ctx.args.body) {
      params = typeof ctx.args.body === 'string' ? JSON.parse(ctx.args.body) : ctx.args.body;
    } else if (ctx.args) {
      const qp = ctx.args.QUERY_PARAMS || ctx.args.queryStringParameters || ctx.args;
      const keys = Object.keys(qp || {});
      if (keys.length > 0) {
        params = {};
        for (const k of keys) params[k] = qp[k];
      }
    }
  } catch (e) {
    return { statusCode: 400, body: JSON.stringify({ success: false, error: 'Invalid params' }) };
  }

  if (!params || !params.code) {
    return { statusCode: 400, body: JSON.stringify({ success: false, error: 'Missing code parameter' }) };
  }

  const { code } = params;

  try {
    // Step 1: 通过 code 换取 access_token
    const tokenUrl = 'https://api.weixin.qq.com/sns/oauth2/access_token'
      + '?appid=' + WECHAT_APP_ID
      + '&secret=' + WECHAT_APP_SECRET
      + '&code=' + encodeURIComponent(code)
      + '&grant_type=authorization_code';

    const tokenRes = await httpGet(tokenUrl);
    if (tokenRes.errcode) {
      const errMap = { 40029: 'code 无效或已过期', 40163: 'code 已被使用', 40001: 'AppSecret 错误', 40003: 'AppID 错误' };
      return { statusCode: 400, body: JSON.stringify({ success: false, error: errMap[tokenRes.errcode] || tokenRes.errmsg || '换取 token 失败', wechatCode: tokenRes.errcode }) };
    }

    const { access_token, refresh_token, openid, unionid } = tokenRes;

    // Step 2: 刷新 access_token
    let finalToken = access_token;
    try {
      const refreshUrl = 'https://api.weixin.qq.com/sns/oauth2/refresh_token'
        + '?appid=' + WECHAT_APP_ID
        + '&grant_type=refresh_token'
        + '&refresh_token=' + refresh_token;

      const refreshRes = await httpGet(refreshUrl);
      if (!refreshRes.errcode && refreshRes.access_token) {
        finalToken = refreshRes.access_token;
      }
    } catch (e) {
      console.log('[authUser] refresh error (continuing with original token):', e.message);
    }

    // Step 3: 获取用户信息
    let nickname = '微信用户';
    let avatarUrl = '';
    try {
      const userInfoUrl = 'https://api.weixin.qq.com/sns/userinfo'
        + '?access_token=' + finalToken
        + '&openid=' + openid
        + '&lang=zh_CN';

      const userInfo = await httpGet(userInfoUrl);
      if (!userInfo.errcode) {
        nickname = userInfo.nickname || '微信用户';
        avatarUrl = userInfo.headimgurl || '';
      }
    } catch (e) {
      console.log('[authUser] userinfo error:', e.message);
    }

    // Step 4: 返回结果
    return {
      statusCode: 200,
      body: JSON.stringify({
        success: true,
        result: { openId: openid, unionId: unionid || openid, nickname: nickname, avatarUrl: avatarUrl }
      })
    };

  } catch (err) {
    console.error('[authUser] fatal error:', err.message);
    return { statusCode: 500, body: JSON.stringify({ success: false, error: '服务内部错误' }) };
  }
};
