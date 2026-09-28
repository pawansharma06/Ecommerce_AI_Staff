import sys, base64, os
rel_path = sys.argv[1]
data = base64.b64decode(sys.argv[2]).decode('utf-8')
mode = sys.argv[3] if len(sys.argv) > 3 else 'w'
base = sys.argv[4] if len(sys.argv) > 4 else r'c:/apps/ShopAI'
full_path = os.path.join(base, rel_path)
os.makedirs(os.path.dirname(full_path), exist_ok=True)
with open(full_path, mode, encoding='utf-8') as f:
    f.write(data)
print('Updated:', rel_path)
