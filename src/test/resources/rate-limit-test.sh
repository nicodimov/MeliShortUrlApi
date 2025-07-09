# Test 10 requests (should all succeed)
for i in {1..20}; do
  echo "Request $i: $(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/v1/shorturl/view/Dm1PJv)"
done

# 11th request should be rate limited (429)
echo "Request 11: $(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/v1/shorturl/view/Dm1PJv)"