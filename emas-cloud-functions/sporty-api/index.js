/**
 * EMAS Serverless 云函数 — 通用数据操作 API
 * 参数支持：控制台直接调用(JSON body) 和 HTTP GET 触发(query string)
 */
module.exports = async (ctx) => {
  let params;

  try {
    // 方式1: 控制台代码执行或 SDK 调用 → body 是 JSON 字符串
    if (ctx.args && ctx.args.body) {
      params = typeof ctx.args.body === 'string'
        ? JSON.parse(ctx.args.body)
        : ctx.args.body;
    }
    // 方式2: HTTP GET 触发 → query 参数在 QUERY_PARAMS 或直接在 args 中
    else if (ctx.args) {
      const qp = ctx.args.QUERY_PARAMS || ctx.args.queryStringParameters || ctx.args;
      const keys = Object.keys(qp);
      if (keys.length > 0) {
        params = {};
        for (const k of keys) {
          let v = qp[k];
          if ((k === 'data' || k === 'query' || k === 'options') && typeof v === 'string') {
            try { params[k] = JSON.parse(v); }
            catch (e) { params[k] = v; }
          } else {
            params[k] = v;
          }
        }
      }
    }

    if (!params || !params.action) {
      return {
        statusCode: 400,
        body: JSON.stringify({
          success: false, error: 'No valid params found',
          debug: { hasArgs: !!ctx.args, argsType: typeof ctx.args, argsKeys: ctx.args ? Object.keys(ctx.args) : [], argsSample: ctx.args ? JSON.stringify(ctx.args).substring(0, 500) : 'null' }
        })
      };
    }
  } catch (e) {
    return { statusCode: 400, body: JSON.stringify({ error: 'Invalid params: ' + e.message }) };
  }

  const { action, collection, data, query, options } = params;

  if (action === 'ping') {
    return { statusCode: 200, body: JSON.stringify({ success: true, message: 'sporty-api running' }) };
  }

  if (!action || !collection) {
    return { statusCode: 400, body: JSON.stringify({ error: 'action and collection are required', received: JSON.stringify(params) }) };
  }

  try {
    const db = ctx.mpserverless.db.collection(collection);
    let result;

    switch (action) {
      case 'insertOne': result = await db.insertOne(data); break;
      case 'insertMany': result = await db.insertMany(data); break;
      case 'find': result = await db.find(query || {}, options || {}); break;
      case 'findOne': result = await db.findOne(query || {}, options || {}); break;
      case 'updateOne': result = await db.updateOne(query, data); break;
      case 'updateMany': result = await db.updateMany(query, data); break;
      case 'deleteOne': result = await db.deleteOne(query); break;
      case 'deleteMany': result = await db.deleteMany(query); break;
      case 'count': result = await db.count(query || {}); break;
      default:
        return { statusCode: 400, body: JSON.stringify({ error: 'Unknown action: ' + action }) };
    }

    return {
      statusCode: 200,
      body: JSON.stringify({ success: result.success !== false, affectedDocs: result.affectedDocs || 0, result: result.result || null })
    };
  } catch (err) {
    return { statusCode: 500, body: JSON.stringify({ error: err.message }) };
  }
};
