import pandas as pd
import numpy as np
import torch
import torch.nn as nn
import torch.optim as optim
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import accuracy_score, log_loss
import json
import time

# --- CONFIGURATION ---
INPUT_FILE = "training_data_multilabel.csv"
OUTPUT_JSON = "nn_weights.json"
METRICS_CSV = "training_metrics.csv"
HISTORY_JSON = "training_history.json"

# --- NETWORKS ---
class FastNet(nn.Module):
    def __init__(self, input_size):
        super(FastNet, self).__init__()
        self.fc1 = nn.Linear(input_size, 32)
        self.relu1 = nn.ReLU()
        self.fc2 = nn.Linear(32, 16)
        self.relu2 = nn.ReLU()
        self.output = nn.Linear(16, 1)
        self.sigmoid = nn.Sigmoid()

    def forward(self, x):
        x = self.relu1(self.fc1(x))
        x = self.relu2(self.fc2(x))
        return self.sigmoid(self.output(x))

class DeepNet(nn.Module):
    def __init__(self, input_size):
        super(DeepNet, self).__init__()
        self.fc1 = nn.Linear(input_size, 128)
        self.relu1 = nn.ReLU()
        self.fc2 = nn.Linear(128, 64)
        self.relu2 = nn.ReLU()
        self.fc3 = nn.Linear(64, 32)
        self.relu3 = nn.ReLU()
        self.fc4 = nn.Linear(32, 16)
        self.relu4 = nn.ReLU()
        self.output = nn.Linear(16, 1)
        self.sigmoid = nn.Sigmoid()

    def forward(self, x):
        x = self.relu1(self.fc1(x))
        x = self.relu2(self.fc2(x))
        x = self.relu3(self.fc3(x))
        x = self.relu4(self.fc4(x))
        return self.sigmoid(self.output(x))

# --- TRAINING ---
def train_and_evaluate(model_class, input_dim, X_train, y_train, X_test, y_test, name):
    print(f"\n--- Training {name} ---")
    start_time = time.time()
    
    model = model_class(input_dim)
    criterion = nn.BCELoss()
    optimizer = optim.Adam(model.parameters(), lr=0.001)
    
    X_t = torch.FloatTensor(X_train)
    y_t = torch.FloatTensor(y_train).unsqueeze(1)
    X_test_t = torch.FloatTensor(X_test)
    y_test_t = torch.FloatTensor(y_test).unsqueeze(1)
    
    loss_history = []
    val_acc_history = []
    
    EPOCHS = 60
    for epoch in range(EPOCHS):
        optimizer.zero_grad()
        outputs = model(X_t)
        loss = criterion(outputs, y_t)
        loss.backward()
        optimizer.step()
        
        # Track History
        loss_history.append(loss.item())
        
        # Track Validation Acc (every epoch)
        with torch.no_grad():
            val_preds = (model(X_test_t) > 0.5).float()
            val_acc = accuracy_score(y_test, val_preds)
            val_acc_history.append(val_acc)

    duration = time.time() - start_time
    
    # Final Metrics
    with torch.no_grad():
        final_preds = (model(X_test_t) > 0.5).float()
        final_acc = accuracy_score(y_test, final_preds)
        # Calculate Validation Loss
        final_probs = model(X_test_t)
        final_loss = criterion(final_probs, y_test_t).item()

    print(f"{name} -> Acc: {final_acc*100:.2f}% | Time: {duration:.2f}s")
    
    metrics = {
        "Model": name,
        "Val_Accuracy": final_acc,
        "Val_Loss": final_loss,
        "Training_Time_Sec": duration,
        "Epochs": EPOCHS
    }
    
    history = {
        "loss": loss_history,
        "val_accuracy": val_acc_history
    }
        
    return model, metrics, history

if __name__ == "__main__":
    print("Loading data...")
    try:
        df = pd.read_csv(INPUT_FILE)
    except FileNotFoundError:
        print(f"Error: {INPUT_FILE} not found.")
        exit()
    
    feature_cols = [c for c in df.columns if c not in ['label_sparse', 'label_dense']]
    print(f"Features: {len(feature_cols)}")
    
    X = df[feature_cols].values
    y_sparse = df['label_sparse'].values
    y_dense = df['label_dense'].values
    
    scaler = StandardScaler()
    X = scaler.fit_transform(X)
    
    indices = np.arange(len(X))
    X_train, X_test, idx_train, idx_test = train_test_split(X, indices, test_size=0.2, random_state=42)
    
    y_sp_train, y_sp_test = y_sparse[idx_train], y_sparse[idx_test]
    y_dn_train, y_dn_test = y_dense[idx_train], y_dense[idx_test]
    
    input_dim = X.shape[1]
    
    all_metrics = []
    all_histories = {}
    
    # Train 4 Models
    m1, met1, hist1 = train_and_evaluate(FastNet, input_dim, X_train, y_sp_train, X_test, y_sp_test, "Sparse_Fast")
    all_metrics.append(met1)
    all_histories["Sparse_Fast"] = hist1
    
    m2, met2, hist2 = train_and_evaluate(DeepNet, input_dim, X_train, y_sp_train, X_test, y_sp_test, "Sparse_Deep")
    all_metrics.append(met2)
    all_histories["Sparse_Deep"] = hist2
    
    m3, met3, hist3 = train_and_evaluate(FastNet, input_dim, X_train, y_dn_train, X_test, y_dn_test, "Dense_Fast")
    all_metrics.append(met3)
    all_histories["Dense_Fast"] = hist3
    
    m4, met4, hist4 = train_and_evaluate(DeepNet, input_dim, X_train, y_dn_train, X_test, y_dn_test, "Dense_Deep")
    all_metrics.append(met4)
    all_histories["Dense_Deep"] = hist4
    
    # Save Files
    pd.DataFrame(all_metrics).to_csv(METRICS_CSV, index=False)
    with open(HISTORY_JSON, 'w') as f: json.dump(all_histories, f)
    
    # Export Weights
    export_data = {
        "scaling": {"mean": scaler.mean_.tolist(), "scale": scaler.scale_.tolist()},
        "sparse_fast": {
            "fc1_w": m1.fc1.weight.data.tolist(), "fc1_b": m1.fc1.bias.data.tolist(),
            "fc2_w": m1.fc2.weight.data.tolist(), "fc2_b": m1.fc2.bias.data.tolist(),
            "out_w": m1.output.weight.data.tolist(), "out_b": m1.output.bias.data.tolist()
        },
        "sparse_deep": {
            "fc1_w": m2.fc1.weight.data.tolist(), "fc1_b": m2.fc1.bias.data.tolist(),
            "fc2_w": m2.fc2.weight.data.tolist(), "fc2_b": m2.fc2.bias.data.tolist(),
            "fc3_w": m2.fc3.weight.data.tolist(), "fc3_b": m2.fc3.bias.data.tolist(),
            "fc4_w": m2.fc4.weight.data.tolist(), "fc4_b": m2.fc4.bias.data.tolist(),
            "out_w": m2.output.weight.data.tolist(), "out_b": m2.output.bias.data.tolist()
        },
        "dense_fast": {
            "fc1_w": m3.fc1.weight.data.tolist(), "fc1_b": m3.fc1.bias.data.tolist(),
            "fc2_w": m3.fc2.weight.data.tolist(), "fc2_b": m3.fc2.bias.data.tolist(),
            "out_w": m3.output.weight.data.tolist(), "out_b": m3.output.bias.data.tolist()
        },
        "dense_deep": {
            "fc1_w": m4.fc1.weight.data.tolist(), "fc1_b": m4.fc1.bias.data.tolist(),
            "fc2_w": m4.fc2.weight.data.tolist(), "fc2_b": m4.fc2.bias.data.tolist(),
            "fc3_w": m4.fc3.weight.data.tolist(), "fc3_b": m4.fc3.bias.data.tolist(),
            "fc4_w": m4.fc4.weight.data.tolist(), "fc4_b": m4.fc4.bias.data.tolist(),
            "out_w": m4.output.weight.data.tolist(), "out_b": m4.output.bias.data.tolist()
        }
    }
    with open(OUTPUT_JSON, 'w') as f: json.dump(export_data, f)
    
    print(f"\nDone! Files created:\n1. {OUTPUT_JSON} (Weights)\n2. {METRICS_CSV} (Report Table)\n3. {HISTORY_JSON} (Graph Data)")