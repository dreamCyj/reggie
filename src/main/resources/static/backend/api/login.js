function loginApi(data) {
  return $axios({
    'url': '/emp/login',
    'method': 'post',
    data
  })
}

function logoutApi(){
  return $axios({
    'url': '/emp/logout',
    'method': 'post',
  })
}
